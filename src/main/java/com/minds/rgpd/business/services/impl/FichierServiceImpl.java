package com.minds.rgpd.business.services.impl;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import com.minds.rgpd.business.Imports.ExcelImportService;
import com.minds.rgpd.business.Imports.ImportResult;
import com.minds.rgpd.business.Imports.ImportSpecification;
import com.minds.rgpd.business.Imports.ImportSpecifications;
import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.InfoFichierDTO.InfoFichierDTOBuilder;
import com.minds.rgpd.business.services.FichierService;
import com.minds.rgpd.business.utilities.mappers.ClientMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Preconisation;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.PreconisationRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final ExcelImportService importer;
    private final ImportSpecifications importSpecifications;
    private final TraitementRepository traitementRepository;
    private final PreconisationRepository preconisationRepository;

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

            // 1. Lecture de la feuille obligatoire "Registre de traitement".
            //    Tant que la lecture n'a pas abouti, aucune écriture n'est faite :
            //    le registre du client reste inchangé.
            ImportSpecification<Traitement> specificationTraitements =
                    importSpecifications.traitement(client, version);
            ImportResult<Traitement> resultTraitements =
                    importer.importSheet(workbook, specificationTraitements);

            if (!specificationTraitements.allowEmpty() && resultTraitements.imported().isEmpty()) {
                String details = resultTraitements.errors().isEmpty()
                        ? "aucune ligne traitable"
                        : resultTraitements.getResultMessage();
                log.warn("Import du fichier {} interrompu : registre du client inchangé ({} : {})",
                        fileName, specificationTraitements.sheetName(), details);
                return infoFichier
                        .dateFinTraitement(LocalDateTime.now())
                        .statusFichier("%s : %s".formatted(specificationTraitements.sheetName(), details));
            }

            // 2. RG2 : l'import remplace l'état précédent des traitements et des
            //    préconisations du client. L'utilisateur en a été averti au préalable
            //    par la modale de confirmation de l'import (RG3).
            remplacerRegistreClient(client);

            // 3. Insertion des traitements du fichier, version portée par le nom
            //    du fichier (ex. "ed3" -> version 3).
            traitementRepository.saveAll(resultTraitements.imported());
            log.info("{} : {} ligne(s) importée(s), {} erreur(s)",
                    specificationTraitements.sheetName(),
                    resultTraitements.imported().size(),
                    resultTraitements.errors().size());

            // RG5 : les colonnes complémentaires du registre, ici la feuille de
            // suivi des préconisations, sont importées lorsqu'elles sont présentes.
            // La lecture se fait après le remplacement des traitements : les liens
            // préconisation -> traitement (par ID ou par nom) se résolvent alors
            // sur les nouvelles lignes, garantissant la cohérence du registre (CA5).
            String preconisationSheet = findPreconisationSheet(workbook);
            if (preconisationSheet != null) {
                ImportSpecification<Preconisation> specificationPreconisations =
                        importSpecifications.preconisation(client, preconisationSheet);
                ImportResult<Preconisation> resultPreconisations =
                        importer.importSheet(workbook, specificationPreconisations);

                preconisationRepository.saveAll(resultPreconisations.imported());
                log.info("{} : {} ligne(s) importée(s), {} erreur(s)",
                        specificationPreconisations.sheetName(),
                        resultPreconisations.imported().size(),
                        resultPreconisations.errors().size());
            }

            // L'import est abouti : le registre du client est remplacé. Les lignes
            // rejetées (données incomplètes) restent détaillées dans les journaux ;
            // le statut retourné reste "OK" pour une importation effective.
            return infoFichier
                    .dateFinTraitement(LocalDateTime.now())
                    .statusFichier("OK");

        } catch (IOException | IllegalArgumentException e) {
            // RG3 : en cas d'échec, l'import est annulé et l'état précédent du
            // registre du client est conservé (transaction rejetée en rollback).
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Import du fichier {} en erreur : transaction annulée, registre du client inchangé", fileName, e);
            return infoFichier.statusFichier(e.getMessage());
        }
    }

    /**
     * RG2 : l'import d'un registre remplace l'état précédent des traitements et
     * des préconisations du client.
     * <p>
     * Seules les données du registre du client importé sont supprimées : les
     * référentiels partagés (établissements, définitions, durées, responsables
     * de traitement) et les données des autres clients sont conservés. Les
     * historisations liées aux traitements supprimés sont purgées par les
     * cascades en base.
     */
    private void remplacerRegistreClient(Client client) {
        int preconisationsSupprimees = preconisationRepository.deleteByClient(client);
        int liensSupprimes = traitementRepository.deleteLiensEtablissementsByClient(client.getId());
        int traitementsSupprimes = traitementRepository.deleteByClient(client);
        log.info("Registre du client {} remplacé : {} traitement(s) et {} préconisation(s) supprimé(s), {} lien(s) établissement purgé(s)",
                client.getNom(), traitementsSupprimes, preconisationsSupprimees, liensSupprimes);
    }

    /**
     * Méthode factory pour créer le bon type de Workbook
     * Utilise le pattern matching avec switch expression (Java 17+)
     */
    private Workbook createWorkbook(String fileName, InputStream inputStream) throws IOException {
        String extension = getFileExtension(fileName);
        // Switch expression (Java 14+)
        // Raison : Plus concis, pas besoin de break, le compilateur vérifie l'exhaustivité
        return switch (extension.toLowerCase()) {
            case EXTENSION_XLSX -> new XSSFWorkbook(inputStream);
            case EXTENSION_XLS -> new HSSFWorkbook(inputStream);
            default -> throw new IllegalArgumentException(
                    "Format de fichier Excel non supporté : %s. Formats acceptés : .xlsx, .xls"
                            .formatted(fileName)
            );
        };
    }

    private String findPreconisationSheet(Workbook workbook) {
        for (String sheetName : PRECONISATION_SHEET_NAMES) {
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
}
