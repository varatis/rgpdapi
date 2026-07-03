package com.minds.rgpd.business.services.impl;
import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.dtos.InfoFichierDTO.InfoFichierDTOBuilder;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.services.FichierService;
import com.minds.rgpd.business.utilities.mappers.ClientMapper;
import com.minds.rgpd.business.utilities.mappers.EtablissementMapper;
import com.minds.rgpd.business.utilities.mappers.RowFileToTraitement;
import com.minds.rgpd.business.utilities.mappers.TraitementMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.EtablissementRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

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
    // Utilisation de groupes nommés (?<name>...) pour plus de lisibilité
    // Permet d'utiliser matcher.group("client") au lieu de matcher.group(1)
    private static final int HEADER_ROWS_TO_SKIP = 6;
    private static final int FIRST_DATA_COLUMN = 1;
    private static final int LAST_DATA_COLUMN = 38;
    private static final int DATA_SHEET_INDEX = 1;
    private static final int CELL_INDEX_NOM = 1;
    private static final int CELL_INDEX_ETABLISSEMENT = 1;
    private static final int CELL_INDEX_FINALITE = 4;
    private static final int CELL_INDEX_GESTIONNAIRE = 5;
    // Extensions de fichiers supportées
    private static final String EXTENSION_XLSX = "xlsx";
    private static final String EXTENSION_XLS = "xls";
    private final TraitementRepository traitementRepository;
    private final TraitementMapper traitementMapper;
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final EtablissementRepository etablissementRepository;
    private final EtablissementMapper etablissementMapper;

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
        log.debug("Client extrait du nom de fichier : {}, version : {}", nomClient, version);

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
        try {
            List<TraitementDTO> traitementDTOList = extractAndProcessData(fichier, clientDTO, version);
            if (traitementDTOList.isEmpty()) {
                log.warn("Aucune ligne traitable dans le fichier");
                return infoFichier.statusFichier("Aucune ligne traitable dans le fichier");
            }
            ImportResult result = saveTraitements(traitementDTOList);
            log.info("Import terminé : {} traitement(s) sauvegardé(s), {} ignoré(s)",
                    result.savedCount(), result.skippedCount());
            return infoFichier
                    .dateFinTraitement(LocalDateTime.now())
                    .statusFichier(buildStatusMessage(result));
        } catch (IOException e) {
            log.error("Erreur lors de la lecture du fichier : {}", e.getMessage(), e);
            return infoFichier.statusFichier("Erreur de lecture : %s".formatted(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Fichier invalide : {}", e.getMessage());
            return infoFichier.statusFichier(e.getMessage());
        }
    }
    /**
     * record pour encapsuler le résultat de l'import
     * Raison : Les records Java 16+ sont parfaits pour les objets de données immutables.
     * Plus concis qu'une classe traditionnelle et génère automatiquement
     * equals(), hashCode(), toString(), et les accesseurs.
     */
    private record ImportResult(int savedCount, int skippedCount) {
        boolean hasSkipped() {
            return skippedCount > 0;
        }
    }
    /**
     * Méthode dédiée pour construire le message de statut
     */
    private String buildStatusMessage(ImportResult result) {
        if (result.hasSkipped()) {
            return "OK - %d traitement(s) déjà existant(s) ignoré(s)".formatted(result.skippedCount());
        }
        return "OK";
    }
    /**
     * Cette méthode combine l'extraction du Sheet ET le traitement des données
     * dans le même try-with-resources, garantissant que le Workbook reste ouvert
     * pendant toute la durée du traitement.
     */
    private List<TraitementDTO> extractAndProcessData(
            MultipartFile fichier,
            ClientDTO clientDTO,
            String version
    ) throws IOException {
        String fileName = Objects.requireNonNull(
                fichier.getOriginalFilename(),
                "Le nom du fichier ne peut pas être null"
        );
        // try-with-resources avec plusieurs ressources
        // Raison : Garantit la fermeture automatique dans le bon ordre (LIFO)
        // même en cas d'exception
        try (InputStream inputStream = fichier.getInputStream();
             Workbook workbook = createWorkbook(fileName, inputStream)) {
            Sheet sheet = workbook.getSheetAt(DATA_SHEET_INDEX);

            // Le traitement se fait ICI, pendant que le Workbook est ouvert
            List<List<String>> rawData = extractData(sheet);
            int versionInt = parseVersion(version);
            return rawData.stream()
                    .map(cellules -> {
                        List<EtablissementDTO> etablissements = findOrCreateEtablissements(cellules, clientDTO);
                        return RowFileToTraitement.map(cellules, etablissements, clientDTO, versionInt);
                    })
                    .toList();
        }
        // Le Workbook est automatiquement fermé ici, APRÈS le traitement
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
    /**
     * Parsing de version avec gestion d'erreur explicite
     */
    private int parseVersion(String version) {
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            log.warn("Version invalide '{}', utilisation de la version 1 par défaut", version);
            return 1;
        }
    }
    /**
     * Extrait les données du Sheet Excel.
     */
    private List<List<String>> extractData(Sheet sheet) {
        Iterable<Row> iterable = sheet::rowIterator;
        return StreamSupport.stream(iterable.spliterator(), false)
                .filter(row -> row.getRowNum() >= HEADER_ROWS_TO_SKIP)
                .filter(this::allMandatoryValuesPresent)
                .map(this::extractRowData)
                .toList();
    }
    /**
     * Extraction des données d'une ligne dans une méthode dédiée
     * Raison : Améliore la lisibilité de extractData()
     */
    private List<String> extractRowData(Row row) {
        List<String> cellArray = new ArrayList<>();
        for (Cell cell : row) {
            int columnIndex = cell.getColumnIndex();
            if (columnIndex >= FIRST_DATA_COLUMN && columnIndex <= LAST_DATA_COLUMN) {
                cellArray.add(getCellValue(cell));
            }
        }
        return cellArray;
    }

    private boolean allMandatoryValuesPresent(Row row) {
        return !getCellValue(row.getCell(CELL_INDEX_NOM)).isBlank()
                && !getCellValue(row.getCell(CELL_INDEX_FINALITE)).isBlank()
                && !getCellValue(row.getCell(CELL_INDEX_GESTIONNAIRE)).isBlank();
    }
    /**
     * Utilisation des Guard Patterns (Java 21)
     * Les guard patterns permettent d'ajouter des conditions aux branches du switch
     * avec le mot-clé 'when'. C'est plus élégant qu'un if imbriqué.
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            // Guard pattern avec variable pattern "CellType ct"
            case CellType ct when ct == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell) ->
                    cell.getDateCellValue().toString();

            // Qualifier avec CellType. pour tous les autres cas
            case CellType.NUMERIC -> formatNumericValue(cell.getNumericCellValue());
            case CellType.STRING -> cell.getStringCellValue();
            case CellType.BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case CellType.FORMULA -> evaluateFormula(cell);

            default -> "";
        };
    }
    /**
     * Extraction du formatage numérique
     * Raison : Évite la notation scientifique pour les grands nombres
     */
    private String formatNumericValue(double value) {
        // Si c'est un entier, on évite le ".0"
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
    /**
     * Gestion des cellules avec formules
     */
    private String evaluateFormula(Cell cell) {
        try {
            CellType cachedType = cell.getCachedFormulaResultType();
            return switch (cachedType) {
                case CellType.STRING -> cell.getStringCellValue();
                case CellType.NUMERIC -> formatNumericValue(cell.getNumericCellValue());
                case CellType.BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                default -> "";
            };
        } catch (Exception e) {
            log.warn("Impossible d'évaluer la formule en cellule [{},{}]",
                    cell.getRowIndex(), cell.getColumnIndex());
            return "";
        }
    }

    @Transactional
    private List<EtablissementDTO> findOrCreateEtablissements(List<String> cellules, ClientDTO client) {
        // Vérification des bornes avant accès à la liste
        if (cellules.isEmpty() || cellules.size() <= CELL_INDEX_ETABLISSEMENT) {
            log.warn("Données insuffisantes pour extraire les établissements");
            return List.of();
        }
        String etablissementsRaw = cellules.get(CELL_INDEX_ETABLISSEMENT);
        return etablissementsRaw.lines()
                .filter(line -> !line.isBlank())
                .map(String::trim)  // Nettoyage des espaces
                .distinct()         // Évite les doublons dans le même fichier
                .map(nom -> findOrCreateEtablissement(nom, client))
                .toList();
    }
    /**
     * Extraction de la logique pour un seul établissement
     */
    private EtablissementDTO findOrCreateEtablissement(String nom, ClientDTO client) {
        return etablissementRepository.findByNom(nom)
                .map(etablissementMapper::map)
                .orElseGet(() -> createAndSaveEtablissement(nom, client));
    }
    /**
     * Séparation création/sauvegarde
     */
    private EtablissementDTO createAndSaveEtablissement(String nom, ClientDTO client) {
        log.debug("Création d'un nouvel établissement : {}", nom);
        EtablissementDTO newEtablissement = EtablissementDTO.builder()
                .id(UUID.randomUUID())
                .nom(nom)
                .client(client)
                .build();
        var savedEntity = etablissementRepository.save(etablissementMapper.map(newEtablissement));
        return etablissementMapper.map(savedEntity);
    }
    /**
     * Méthode dédiée pour la sauvegarde avec déduplication
     */
    private ImportResult saveTraitements(List<TraitementDTO> traitementDTOList) {
        List<Traitement> traitements = traitementMapper.mapToTraitementList(traitementDTOList);
        List<Traitement> traitementsToSave = new ArrayList<>();
        int skippedCount = 0;
        for (Traitement traitement : traitements) {
            if (isNewTraitement(traitement)) {
                traitementsToSave.add(traitement);
            } else {
                skippedCount++;
                log.trace("Traitement déjà existant ignoré : {}", traitement.getNom());
            }
        }
        if (!traitementsToSave.isEmpty()) {
            traitementRepository.saveAll(traitementsToSave);
        }
        return new ImportResult(traitementsToSave.size(), skippedCount);
    }
    /**
     * Extraction du test d'existence
     */
    private boolean isNewTraitement(Traitement traitement) {
        // Retourne true si la liste est vide (aucun doublon trouvé)
        List<Traitement> existing = traitementRepository.findByAllBusinessFields(
                traitement.getNom(),
                traitement.getClient(),
                traitement.getGestionnaire(),
                traitement.getFinalitePrincipale(),
                traitement.getDateIdentification()
        );

        if (!existing.isEmpty()) {
            log.debug("Traitement déjà existant trouvé ({} occurrence(s)) : {}",
                    existing.size(), traitement.getNom());
        }

        return existing.isEmpty();
    }

}