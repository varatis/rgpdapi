package com.minds.rgpd.business.services.impl;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.minds.rgpd.persistence.repositories.ClientRepository;

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
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final ExcelImportService importer;
    private final ImportSpecifications importSpecifications;

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
            
            List<ImportSpecification<?>> specifications = List.of(importSpecifications.traitement(client, version));
            List<String> messages = new ArrayList<>();
            boolean hasErrors = false;
            for (ImportSpecification<?> specification : specifications) {
                ImportReport report = runImport(workbook, specification);
                messages.add(report.message());
                hasErrors = hasErrors || !report.successful();
            }

            // On annule la transaction dès qu'une erreur est présente.
            if (hasErrors) {
                log.warn("Import du fichier {} en erreur : transaction annulée, aucune donnée persistée", fileName);
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            String ok = "OK";
            return infoFichier
                    .dateFinTraitement(LocalDateTime.now())
                    .statusFichier(ok);

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
            return new ImportReport(
                    "%s : aucune ligne traitable".formatted(specification.sheetName()),
                    false);
        }

        // On ne persiste qu'une feuille intègre. Si elle contient des erreurs, on ne
        // l'enregistre pas et l'appelant annulera la transaction globale (voir processImport).
        if (result.isSuccessful()) {
            int saved = persist(specification, result.imported());
            log.info("{} : {} ligne(s) lue(s), {} sauvegardée(s)",
                    specification.sheetName(), result.imported().size(), saved);
        }

        return new ImportReport(
                "%s : %s".formatted(specification.sheetName(), result.getResultMessage()),
                result.isSuccessful());
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
    /**
     * Extraction de l'extension de fichier dans une méthode utilitaire
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
}