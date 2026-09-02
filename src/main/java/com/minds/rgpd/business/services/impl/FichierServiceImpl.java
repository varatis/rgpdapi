package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.Imports.ExcelImportService;
import com.minds.rgpd.business.Imports.ImportResult;
import com.minds.rgpd.business.Imports.ImportSpecification;
import com.minds.rgpd.business.Imports.ImportSpecifications;
import com.minds.rgpd.business.dtos.*;
import com.minds.rgpd.business.dtos.InfoFichierDTO.InfoFichierDTOBuilder;
import com.minds.rgpd.business.services.FichierService;
import com.minds.rgpd.business.services.HistorisationService;
import com.minds.rgpd.business.utilities.mappers.ClientMapper;
import com.minds.rgpd.business.utilities.mappers.TraitementMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.PreconisationRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import com.minds.rgpd.persistence.repositories.ViolationRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
            "^(?<client>[^_]+)_(?<etablissement>[^_]+)_Registre RGPD_ed(?<version>.+)\\.(?<extension>xlsx|xls)$",
            Pattern.CASE_INSENSITIVE
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
    private static final String SHEET_REGISTRE = "Registre de traitement";

    private static final int HEADER_ROW_INDEX = 5;
    private static final int FIRST_COLUMN = 1;

    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] EXPORT_HEADERS = {
            "ID",
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
            "Commentaires",
            "Impact du traitement",
            "Détournement de finalité",
            "Score",
            "Collecte de DCP inapprorpiées",
            "Score2",
            "Conservation excessive de DCP",
            "Score3",
            "Sécurisation insuffisante des DCP",
            "Score4",
            "Vices du consentement",
            "Score5",
            "Manque de transparence",
            "Score6",
            "Incapacité à permettre l'exercice des droits",
            "Score7",
            "Transfert auprès d'un tiers mal encadré",
            "Score8",
            "Transfert hors UE abusif",
            "score9",
            "Défaut de preuve",
            "Score10",
            "Score global",
            "Commentaires analyse",
            "Exposition du traitement",
            "évaluation / scoring",
            "décision automatique",
            "surveillance systématique",
            "collecte de données sensibles",
            "collecte de données personnelles à large échelle",
            "croisement de données",
            "personnes vulnérables",
            "usage innovant",
            "exclusion du bénéfice d’un droit/contrat"
    };

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final ExcelImportService importer;
    private final ImportSpecifications importSpecifications;
    private final TraitementRepository traitementRepository;
    private final PreconisationRepository preconisationRepository;
    private final ViolationRepository violationRepository;
    private final TraitementMapper traitementMapper;
    private final HistorisationService historisationService;

    // @Transactional explicite pour les méthodes d'écriture
    @Override
    @Transactional
    public InfoFichierDTOBuilder importFichier(MultipartFile fichier, InfoFichierDTOBuilder infoFichier) {
        return importFichier(fichier, infoFichier, false);
    }

    @Override
    @Transactional
    public InfoFichierDTOBuilder importFichier(
            MultipartFile fichier,
            InfoFichierDTOBuilder infoFichier,
            boolean confirmerRemplacement) {
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
                .map(client -> {
                    ImportApercuDTO apercu = construireApercu(originalFilename, client, version);
                    if (apercu.remplacementDonnees() && !confirmerRemplacement) {
                        log.info("Import du fichier {} suspendu : confirmation du remplacement requise",
                                originalFilename);
                        return infoFichier
                                .statusFichier(apercu.avertissement())
                                .confirmationRequise(true)
                                .apercu(apercu);
                    }
                    return processImport(fichier, client, version, infoFichier);
                })
                .orElseGet(() -> {
                    log.warn("Client non trouvé en base : {}", nomClient);
                    return infoFichier.statusFichier(
                            "Client absent de la base de données : %s".formatted(nomClient)
                    );
                });
    }

    @Override
    public ImportApercuDTO apercuImport(String nomFichier) {
        if (Objects.isNull(nomFichier) || nomFichier.isBlank()) {
            return apercuInvalide(nomFichier, "Le nom du fichier est null");
        }
        Matcher matcher = FILENAME_PATTERN.matcher(nomFichier);
        if (!matcher.matches()) {
            return apercuInvalide(nomFichier,
                    "Le fichier %s n'a pas le nom formaté comme attendu.".formatted(nomFichier));
        }
        String nomClient = matcher.group("client");
        String version = matcher.group("version");
        return clientRepository.findByNom(nomClient)
                .map(client -> construireApercu(nomFichier, client, version))
                .orElseGet(() -> apercuInvalide(nomFichier,
                        "Client absent de la base de données : %s".formatted(nomClient)));
    }

    private ImportApercuDTO apercuInvalide(String nomFichier, String message) {
        return ImportApercuDTO.builder()
                .nomFichier(nomFichier)
                .fichierValide(false)
                .messageErreur(message)
                .remplacementDonnees(false)
                .build();
    }

    private ImportApercuDTO construireApercu(String nomFichier, Client client, String versionFichier) {
        long traitements = traitementRepository.countByClient(client);
        long preconisations = preconisationRepository.findByClient(client).size();
        long violations = violationRepository.findByClient(client).size();
        boolean remplacement = traitements > 0 || preconisations > 0 || violations > 0;

        String avertissement = remplacement
                ? ("L'import va remplacer la totalité du registre de %s : %d traitement(s), "
                + "%d préconisation(s) et %d violation(s) enregistrés seront supprimés et "
                + "remplacés par le contenu du fichier. Exportez le registre actuel avant de confirmer.")
                .formatted(client.getNom(), traitements, preconisations, violations)
                : "Le registre de %s est vide : l'import créera les données du fichier.".formatted(client.getNom());

        return ImportApercuDTO.builder()
                .nomFichier(nomFichier)
                .clientNom(client.getNom())
                .fichierValide(true)
                .versionActuelle(client.getVersion())
                .dateVersionActuelle(client.getDateVersion())
                .versionFichier(versionFichier)
                .remplacementDonnees(remplacement)
                .nombreTraitementsExistants(traitements)
                .nombrePreconisationsExistantes(preconisations)
                .nombreViolationsExistantes(violations)
                .avertissement(avertissement)
                .urlExportPrealable("/importFichierRgpd/export")
                .build();
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

            int remplaces = remplacerRegistre(client);

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
            int importes = 0;
            for (ImportSpecification<?> specification : specifications) {
                ImportReport report = runImport(workbook, specification);
                messages.add(report.message());
                hasErrors = hasErrors || !report.successful();
                if (SHEET_REGISTRE.equals(specification.sheetName())) {
                    importes = report.lignesEnregistrees();
                }
            }

            String returnedMessage = "OK";
            // Rollback uniquement si une feuille obligatoire n'a produit aucune ligne.
            if (hasErrors) {
                log.warn("Import du fichier {} en erreur : transaction annulée, aucune donnée persistée", fileName);
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                returnedMessage = String.join("\n", messages);
                return infoFichier
                        .dateFinTraitement(LocalDateTime.now())
                        .statusFichier(returnedMessage);
            }

            mettreAJourVersionRegistre(client, version);

            historisationService.historiserRegistre(client,
                    "%s (%s) : %d traitement(s) remplacé(s) par %d traitement(s), version du registre %s"
                            .formatted(HistorisationService.MOTIF_IMPORT, fileName, remplaces, importes, version));

            return infoFichier
                    .dateFinTraitement(LocalDateTime.now())
                    .statusFichier(returnedMessage)
                    .version(client.getVersion())
                    .nombreTraitementsRemplaces(remplaces)
                    .nombreTraitementsImportes(importes);

        } catch (IOException e) {
            log.error("Erreur lors de la lecture du fichier : {}", e.getMessage(), e);
            return infoFichier.statusFichier("Erreur de lecture : %s".formatted(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Fichier invalide : {}", e.getMessage());
            return infoFichier.statusFichier(e.getMessage());
        }
    }

    private int remplacerRegistre(Client client) {
        List<Traitement> existants = traitementRepository.findByClient(client);
        preconisationRepository.deleteAll(preconisationRepository.findByClient(client));
        violationRepository.deleteAll(violationRepository.findByClient(client));
        traitementRepository.deleteAll(existants);
        traitementRepository.flush();
        log.info("Registre du client {} remplacé : {} traitement(s) supprimé(s)",
                client.getNom(), existants.size());
        return existants.size();
    }

    private void mettreAJourVersionRegistre(Client client, String version) {
        if (Objects.isNull(version) || version.isBlank()) {
            return;
        }
        String versionNettoyee = version.trim();
        if (versionNettoyee.equals(client.getVersion())) {
            client.setDateVersion(LocalDate.now());
            clientRepository.save(client);
            return;
        }
        log.info("Version du registre du client {} : {} -> {}",
                client.getNom(), client.getVersion(), versionNettoyee);
        client.setVersion(versionNettoyee);
        client.setDateVersion(LocalDate.now());
        clientRepository.save(client);
    }

    /** Résultat d'une feuille : message pour le rapport + statut (aucune erreur ?). */
    /** Résultat d'une feuille : message pour le rapport + statut (aucune erreur ?). */
    private record ImportReport(String message, boolean successful, int lignesEnregistrees) {}

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
                    false,
                    0);
        }

        // On persiste les lignes valides même si d'autres lignes de la feuille sont incomplètes.
        // Un rollback global n'a lieu que si une feuille obligatoire n'a aucune ligne importable.
        int saved = persist(specification, result.imported());
        log.info("{} : {} ligne(s) lue(s), {} sauvegardée(s), {} erreur(s)",
                specification.sheetName(), result.imported().size(), saved, result.errors().size());

        if (result.isSuccessful()) {
            return new ImportReport("OK", true, saved);
        }

        return new ImportReport(
                "%s : %s (%d ligne(s) importée(s))".formatted(
                        specification.sheetName(), result.getResultMessage(), saved),
                true,
                saved);
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

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(SHEET_REGISTRE);

            sheet.createRow(0).createCell(FIRST_COLUMN)
                    .setCellValue("Grille de collecte / Registre des activités de traitement - " + client.nom());

            Row headerRow = sheet.createRow(HEADER_ROW_INDEX);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                headerRow.createCell(FIRST_COLUMN + i).setCellValue(EXPORT_HEADERS[i]);
            }

            int rowIndex = HEADER_ROW_INDEX + 1;
            for (TraitementDTO traitement : traitementList) {
                ecrireLigneTraitement(sheet.createRow(rowIndex++), traitement);
            }

            // Automatically size columns
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(FIRST_COLUMN + i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void ecrireLigneTraitement(Row row, TraitementDTO traitement) {
        int colonne = FIRST_COLUMN;

        ecrire(row, colonne++, traitement.idFonctionnel());
        ecrire(row, colonne++, traitement.etablissements() == null
                ? ""
                : traitement.etablissements().stream()
                        .map(EtablissementDTO::nom)
                        .collect(java.util.stream.Collectors.joining(System.lineSeparator())));
        ecrire(row, colonne++, traitement.donneesConcernees());
        ecrire(row, colonne++, traitement.nom());
        ecrireDate(row, colonne++, traitement.dateIdentification());
        ecrireDate(row, colonne++, traitement.dateMiseAJour());
        ecrire(row, colonne++, traitement.historiqueModifications());
        ecrire(row, colonne++, traitement.dataProtectionOfficer());
        ecrire(row, colonne++, valeurDe(traitement.responsableTraitement()));
        ecrire(row, colonne++, traitement.gestionnaireMiseEnOeuvre());
        ecrire(row, colonne++, valeurDe(traitement.finalitePrincipale()));
        ecrire(row, colonne++, traitement.sousFinalites());
        ecrire(row, colonne++, traitement.categoriesPersonnesConcernees());
        ecrire(row, colonne++, traitement.donneesIdentification());
        ecrire(row, colonne++, traitement.donneesConnexion());
        ecrire(row, colonne++, traitement.donneesLocalisation());
        ecrire(row, colonne++, traitement.donneesComportementViePerso());
        ecrire(row, colonne++, traitement.donneesEconomiquesFinancieres());
        ecrire(row, colonne++, traitement.donneesProfessionnelles());
        ecrire(row, colonne++, traitement.categoriesParticulieresDonnees());
        ecrire(row, colonne++, valeurDe(traitement.sensibilite()));
        ecrire(row, colonne++, valeurDe(traitement.etudeImpact()));
        ecrire(row, colonne++, traitement.canauxCollecteDonnees());
        ecrire(row, colonne++, valeurDe(traitement.licieteTraitement()));
        ecrireOuiNon(row, colonne++, traitement.recoursTraitementAutomatises());
        ecrire(row, colonne++, traitement.emplacementPhysique());
        ecrire(row, colonne++, traitement.dispositionsSecuriteDonneesPhysique());
        ecrire(row, colonne++, traitement.emplacementNumerique());
        ecrire(row, colonne++, traitement.dispositionsSecuriteDonneesNumerique());
        ecrire(row, colonne++, traitement.hebergement());
        ecrire(row, colonne++, valeurDe(traitement.dureeConservation()));
        ecrireOuiNon(row, colonne++, traitement.archivage());
        ecrire(row, colonne++, valeurDe(traitement.dureeArchivage()));
        ecrire(row, colonne++, traitement.categoriesDestinataires());
        ecrire(row, colonne++, traitement.raisonsTransfertDestinataires());
        ecrireOuiNon(row, colonne++, traitement.transfertsHorsUE());
        ecrire(row, colonne++, traitement.paysDestinataires());
        ecrire(row, colonne++, traitement.commentaires());

        ecrire(row, colonne++, traitement.impactTraitement());
        ecrire(row, colonne++, traitement.detournementFinalite());
        ecrire(row, colonne++, traitement.scoreDetournementFinalite());
        ecrire(row, colonne++, traitement.collecteDcpInappropriees());
        ecrire(row, colonne++, traitement.scoreCollecteDcpInappropriees());
        ecrire(row, colonne++, traitement.conservationExcessiveDcp());
        ecrire(row, colonne++, traitement.scoreConservationExcessiveDcp());
        ecrire(row, colonne++, traitement.securisationInsuffisanteDcp());
        ecrire(row, colonne++, traitement.scoreSecurisationInsuffisanteDcp());
        ecrire(row, colonne++, traitement.vicesConsentement());
        ecrire(row, colonne++, traitement.scoreVicesConsentement());
        ecrire(row, colonne++, traitement.manqueTransparence());
        ecrire(row, colonne++, traitement.scoreManqueTransparence());
        ecrire(row, colonne++, traitement.incapaciteExerciceDroits());
        ecrire(row, colonne++, traitement.scoreIncapaciteExerciceDroits());
        ecrire(row, colonne++, traitement.transfertTiersMalEncadre());
        ecrire(row, colonne++, traitement.scoreTransfertTiersMalEncadre());
        ecrire(row, colonne++, traitement.transfertHorsUeAbusif());
        ecrire(row, colonne++, traitement.scoreTransfertHorsUeAbusif());
        ecrire(row, colonne++, traitement.defautPreuve());
        ecrire(row, colonne++, traitement.scoreDefautPreuve());
        ecrire(row, colonne++, traitement.scoreGlobal());
        ecrire(row, colonne++, traitement.commentairesAnalyse());
        ecrire(row, colonne++, traitement.expositionTraitement());

        ecrireCroix(row, colonne++, traitement.critereEvaluationScoring());
        ecrireCroix(row, colonne++, traitement.critereDecisionAutomatique());
        ecrireCroix(row, colonne++, traitement.critereSurveillanceSystematique());
        ecrireCroix(row, colonne++, traitement.critereCollecteDonneesSensibles());
        ecrireCroix(row, colonne++, traitement.critereCollecteLargeEchelle());
        ecrireCroix(row, colonne++, traitement.critereCroisementDonnees());
        ecrireCroix(row, colonne++, traitement.criterePersonnesVulnerables());
        ecrireCroix(row, colonne++, traitement.critereUsageInnovant());
        ecrireCroix(row, colonne, traitement.critereExclusionBeneficeDroit());
    }

    private void ecrire(Row row, int colonne, Object valeur) {
        row.createCell(colonne).setCellValue(Objects.isNull(valeur) ? "" : String.valueOf(valeur));
    }

    private void ecrireDate(Row row, int colonne, LocalDate date) {
        row.createCell(colonne).setCellValue(Objects.isNull(date) ? "" : date.format(EXPORT_DATE_FORMAT));
    }

    private void ecrireOuiNon(Row row, int colonne, Boolean valeur) {
        String texte = Objects.isNull(valeur) ? "" : (Boolean.TRUE.equals(valeur) ? "Oui" : "Non");
        row.createCell(colonne).setCellValue(texte);
    }

    private void ecrireCroix(Row row, int colonne, Boolean valeur) {
        row.createCell(colonne).setCellValue(Boolean.TRUE.equals(valeur) ? "X" : "");
    }

    private String valeurDe(DefinitionDTO definition) {
        return Objects.isNull(definition) ? "" : definition.valeur();
    }

    private String valeurDe(DureeDTO duree) {
        return Objects.isNull(duree) ? "" : duree.valeur();
    }

    private String valeurDe(ResponsableTraitementDTO responsable) {
        return Objects.isNull(responsable) ? "" : responsable.valeur();
    }
}
