package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.Imports.ExcelImportService;
import com.minds.rgpd.business.Imports.ImportResult;
import com.minds.rgpd.business.Imports.ImportSpecification;
import com.minds.rgpd.business.Imports.ImportSpecifications;
import com.minds.rgpd.business.dtos.*;
import com.minds.rgpd.business.dtos.InfoFichierDTO.InfoFichierDTOBuilder;
import com.minds.rgpd.business.services.FichierService;
import com.minds.rgpd.business.utilities.mappers.ClientMapper;
import com.minds.rgpd.business.utilities.mappers.TraitementMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import com.minds.rgpd.persistence.specifications.TraitementSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
// @Transactional(readOnly = true) par défaut
// Raison : La plupart des opérations sont en lecture. On surcharge avec @Transactional
// uniquement sur les méthodes qui écrivent. Cela améliore les performances car Spring
// peut optimiser les transactions en lecture seule (pas de flush automatique, etc.)
@Transactional(readOnly = true)
public class FichierServiceImpl implements FichierService {
    // Raison : La compilation d'une regex est coûteuse. En la compilant une seule
    // fois au chargement de la classe, on évite de recompiler à chaque appel.
    private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "^(?<client>[^_]+)_(?<etablissement>[^_]+)_Registre RGPD_ed(?<version>[^.]+)\\.[^.]+\\.[a-z]+$"
    );

    // Extensions de fichiers supportées
    private static final String EXTENSION_XLSX = "xlsx";
    private static final String EXTENSION_XLS = "xls";
    private static final List<String> PRECONISATION_SHEET_NAMES = List.of(
            "Suivi des préconisations",
            "Préconisations"
    );
    private static final List<String> VIOLATION_SHEET_NAMES = List.of(
            "Recueil de violation",
            "Recueil de violations",
            "Registre des violations"
    );
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final ExcelImportService importer;
    private final ImportSpecifications importSpecifications;
    private final TraitementRepository traitementRepository;
    private final TraitementMapper traitementMapper;

    // @Transactional explicite pour les méthodes d'écriture
    @Override
    @Transactional
    public InfoFichierDTOBuilder importFichier(MultipartFile fichier, InfoFichierDTOBuilder infoFichier) {
        log.info("Début de l'import du fichier : {}", fichier.getOriginalFilename());
        String originalFilename = fichier.getOriginalFilename();

        if (Objects.isNull(originalFilename)) {
            log.error("Le nom du fichier est null");
            return infoFichier.statusFichier("Le nom du fichier est null");
        }
        Matcher matcher = FILENAME_PATTERN.matcher(originalFilename);
        if (!matcher.matches()) {
            log.warn("Format de nom de fichier invalide : {}", originalFilename);
            return infoFichier.statusFichier(
                    "Le fichier %s n'a pas le nom formaté comme attendu.".formatted(originalFilename)
            );
        }

        String nomClient = matcher.group("client");
        String version = matcher.group("version");
        log.info("Client extrait du nom de fichier : {}, version : {}", nomClient, version);

        return clientRepository.findByNom(nomClient)
                .map(client -> processImport(fichier, client, version, infoFichier))
                .orElseGet(() -> {
                    log.warn("Client non trouvé en base : {}", nomClient);
                    return infoFichier.statusFichier(
                            "Client absent de la base de données : %s".formatted(nomClient)
                    );
                });
    }

    private InfoFichierDTOBuilder processImport(
            MultipartFile fichier,
            Client client,
            String version,
            InfoFichierDTOBuilder infoFichier
    ) {
        ClientDTO clientDTO = clientMapper.map(client);
        log.debug("Traitement du fichier pour le client : {}", clientDTO.nom());
        String fileName = Objects.requireNonNull(
                fichier.getOriginalFilename(),
                "Le nom du fichier ne peut pas être null"
        );
        try(InputStream inputStream = fichier.getInputStream();
            Workbook workbook = createWorkbook(fileName, inputStream)) {

            List<ImportSpecification<?>> specifications = new ArrayList<>();
            specifications.add(importSpecifications.traitement(client, version));
            String preconisationSheet = findSheet(workbook, PRECONISATION_SHEET_NAMES);
            if (preconisationSheet != null) {
                specifications.add(importSpecifications.preconisation(client, preconisationSheet));
            }
            // Feuille facultative : absente de certains registres, et vide dans la plupart.
            String violationSheet = findSheet(workbook, VIOLATION_SHEET_NAMES);
            if (violationSheet != null) {
                specifications.add(importSpecifications.violation(client, violationSheet));
            }

            List<String> messages = new ArrayList<>();
            boolean hasErrors = false;
            for (ImportSpecification<?> specification : specifications) {
                ImportReport report = runImport(workbook, specification);
                messages.add(report.message());
                hasErrors = hasErrors || !report.successful();
            }

            String returnedMessage = "OK";
            // Rollback uniquement si une feuille obligatoire n'a produit aucune ligne.
            if (hasErrors) {
                log.warn("Import du fichier {} en erreur : transaction annulée, aucune donnée persistée", fileName);
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                returnedMessage = String.join("\n", messages);
            }
            return infoFichier
                    .dateFinTraitement(LocalDateTime.now())
                    .statusFichier(returnedMessage);

        } catch (IOException e) {
            log.error("Erreur lors de la lecture du fichier : {}", e.getMessage(), e);
            return infoFichier.statusFichier("Erreur de lecture : %s".formatted(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Fichier invalide : {}", e.getMessage());
            return infoFichier.statusFichier(e.getMessage());
        }
    }

    /** Résultat d'une feuille : message pour le rapport + statut (aucune erreur ?). */
    private record ImportReport(String message, boolean successful) {}

    /**
     * Méthode générique : le paramètre de type T est capturé ici, ce qui permet à
     * l'appelant de manipuler des ImportSpecification&lt;?&gt; dans une liste hétérogène.
     */
    private <T> ImportReport runImport(Workbook workbook, ImportSpecification<T> specification) {
        ImportResult<T> result = importer.importSheet(workbook, specification);

        if (!specification.allowEmpty() && result.imported().isEmpty()) {
            String details = result.errors().isEmpty()
                    ? "aucune ligne traitable"
                    : result.getResultMessage();
            return new ImportReport(
                    "%s : %s".formatted(specification.sheetName(), details),
                    false);
        }

        // On persiste les lignes valides même si d'autres lignes de la feuille sont incomplètes.
        // Un rollback global n'a lieu que si une feuille obligatoire n'a aucune ligne importable.
        int saved = persist(specification, result.imported());
        log.info("{} : {} ligne(s) lue(s), {} sauvegardée(s), {} erreur(s)",
                specification.sheetName(), result.imported().size(), saved, result.errors().size());

        if (result.isSuccessful()) {
            return new ImportReport("OK", true);
        }

        return new ImportReport(
                "%s : %s (%d ligne(s) importée(s))".formatted(
                        specification.sheetName(), result.getResultMessage(), saved),
                true);
    }

    private <T> int persist(ImportSpecification<T> spec, List<T> items) {
        List<T> toSave = items.stream()
                .filter(item -> !spec.isDuplicate().test(item))
                .toList();
        if (!toSave.isEmpty()) {
            spec.repository().saveAll(toSave);
        }
        return toSave.size();
    }

    /**
     * Méthode factory pour créer le bon type de Workbook
     * Utilise le pattern matching avec switch expression (Java 17+)
     */
    private Workbook createWorkbook(String fileName, InputStream inputStream) throws IOException {
        String extension = getFileExtension(fileName);
        // Switch expression (Java 14+)
        // Raison : Plus concis, pas besoin de break, et le compilateur vérifie l'exhaustivité
        return switch (extension.toLowerCase()) {
            case EXTENSION_XLSX -> new XSSFWorkbook(inputStream);
            case EXTENSION_XLS -> new HSSFWorkbook(inputStream);
            default -> throw new IllegalArgumentException(
                    "Format de fichier Excel non supporté : %s. Formats acceptés : .xlsx, .xls"
                            .formatted(fileName)
            );
        };
    }

    private String findSheet(Workbook workbook, List<String> candidateNames) {
        for (String sheetName : candidateNames) {
            if (workbook.getSheet(sheetName) != null) {
                return sheetName;
            }
        }
        return null;
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    @Override
    public byte[] generationExcelRegistreTraitements(ClientDTO client, String fileName) throws IOException {

        // Récupération des traitements
        Specification<Traitement> spec = TraitementSpecifications.search(client.nom(), null,  null, null);
        List<TraitementDTO> traitementList = traitementMapper.mapToDTOList(traitementRepository.findAll(spec));

        Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Sheet sheet = workbook.createSheet("Registre de traitement");
        Row headerRow = sheet.createRow(0);

        String[] headers = {
                "id",
                "Etablissement(s)",
                "Données concernées",
                "Nom du traitement",
                "Date d'identification du traitement",
                "Date de mise à jour",
                "Historique des modifications",
                "Data Protection Officer",
                "Responsable de traitement",
                "Gestionnaire de la mise en œuvre du traitement",
                "Finalité principale",
                "Sous-finalités",
                "Catégories de personnes concernées par le traitement",
                "Données d'identification",
                "Données de connexion",
                "Données de localisation",
                "Données sur le comportement et la vie personnelle",
                "Données économiques et financières",
                "Données professionnelles",
                "Catégories particulières de données (NIR, santé par exemple)",
                "Sensibilité",
                "Etude d'impact (PIA)",
                "Canaux de collecte des données",
                "Licéité du traitement",
                "Recours au traitements automatisés (y compris profilage) ? (Oui / Non)",
                "Emplacement physique du traitement",
                "Dispositions existantes pour assurer la sécurité des données",
                "Emplacement numérique du traitement",
                "Dispositions existantes pour assurer la sécurité des données",
                "Hébergement",
                "Durée de conservation",
                "Archivage ? (Oui / Non)",
                "Durée d'archivage",
                "Catégories de destinataires",
                "Raisons du transfert vers les catégories de destinataires",
                "Transferts hors UE (Oui / Non)",
                "Pays destinataires",
                "Commentaires"
        };

        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Data
        int rowIndex = 1;

        for (TraitementDTO traitement : traitementList) {
            Row row = sheet.createRow(rowIndex++);

            StringBuilder etablissementsSb = new StringBuilder();
            for(EtablissementDTO ets : traitement.etablissements()){
                etablissementsSb.append(ets.nom()).append(", ");
            }

            row.createCell(0).setCellValue(traitement.idFonctionnel());
            row.createCell(1).setCellValue(etablissementsSb.toString());
            row.createCell(2).setCellValue(Objects.toString(traitement.donneesConcernees(), ""));
            row.createCell(3).setCellValue(Objects.toString(traitement.nom(),""));
            row.createCell(4).setCellValue(Objects.toString(traitement.dateIdentification(),""));
            row.createCell(5).setCellValue(Objects.toString(traitement.dateMiseAJour(),""));
            row.createCell(6).setCellValue(Objects.toString(traitement.historiqueModifications(),""));
            row.createCell(7).setCellValue(Objects.toString(traitement.dataProtectionOfficer(),""));

            ResponsableTraitementDTO respTr = traitement.responsableTraitement();
            String respTrStr = respTr == null ? "" : respTr.valeur();
            row.createCell(8).setCellValue(respTrStr);
            row.createCell(9).setCellValue(Objects.toString(traitement.gestionnaireMiseEnOeuvre(),""));

            DefinitionDTO finPr = traitement.finalitePrincipale();
            String finPrStr = finPr == null ? "" : finPr.valeur();
            row.createCell(10).setCellValue(finPrStr);

            row.createCell(11).setCellValue(Objects.toString(traitement.sousFinalites(),""));
            row.createCell(12).setCellValue(Objects.toString(traitement.categoriesPersonnesConcernees(),""));
            row.createCell(13).setCellValue(Objects.toString(traitement.donneesIdentification(),""));
            row.createCell(14).setCellValue(Objects.toString(traitement.donneesConnexion(),""));
            row.createCell(15).setCellValue(Objects.toString(traitement.donneesLocalisation(),""));
            row.createCell(16).setCellValue(Objects.toString(traitement.donneesComportementViePerso(),""));
            row.createCell(17).setCellValue(Objects.toString(traitement.donneesEconomiquesFinancieres(),""));
            row.createCell(18).setCellValue(Objects.toString(traitement.donneesProfessionnelles(),""));
            row.createCell(19).setCellValue(Objects.toString(traitement.categoriesParticulieresDonnees(),""));

            DefinitionDTO sensib = traitement.sensibilite();
            String sensibStr = sensib == null ? "" : sensib.valeur();
            row.createCell(20).setCellValue(sensibStr);

            DefinitionDTO etImp = traitement.etudeImpact();
            String etImpStr = etImp == null ? "" : etImp.valeur();
            row.createCell(21).setCellValue(etImpStr);
            row.createCell(22).setCellValue(Objects.toString(traitement.canauxCollecteDonnees(),""));

            DefinitionDTO licTr = traitement.licieteTraitement();
            String licTrStr = licTr == null ? "" : licTr.valeur();
            row.createCell(23).setCellValue(licTrStr);
            row.createCell(24).setCellValue(Objects.toString(traitement.recoursTraitementAutomatises(),""));
            row.createCell(25).setCellValue(Objects.toString(traitement.emplacementPhysique(),""));
            row.createCell(26).setCellValue(Objects.toString(traitement.dispositionsSecuriteDonneesPhysique(),""));
            row.createCell(27).setCellValue(Objects.toString(traitement.emplacementNumerique(),""));
            row.createCell(28).setCellValue(Objects.toString(traitement.dispositionsSecuriteDonneesNumerique(),""));
            row.createCell(29).setCellValue(Objects.toString(traitement.hebergement(),""));

            DureeDTO durCons = traitement.dureeConservation();
            String durConsStr = durCons == null ? "" : durCons.valeur();
            row.createCell(30).setCellValue(durConsStr);

            row.createCell(31).setCellValue(Objects.toString(traitement.archivage(),"non"));

            DureeDTO durArc = traitement.dureeArchivage();
            String durArcStr = durArc == null ? "" : durArc.valeur();
            row.createCell(32).setCellValue(durArcStr);
            row.createCell(33).setCellValue(Objects.toString(traitement.categoriesDestinataires(),""));
            row.createCell(34).setCellValue(Objects.toString(traitement.raisonsTransfertDestinataires(),""));
            row.createCell(35).setCellValue(Objects.toString(traitement.transfertsHorsUE(),""));
            row.createCell(36).setCellValue(Objects.toString(traitement.paysDestinataires(),""));
            row.createCell(37).setCellValue(Objects.toString(traitement.commentaires(),""));
        }

        // Automatically size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(outputStream);

        return outputStream.toByteArray();
    }
}