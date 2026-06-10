package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.dtos.InfoFichierDTO.InfoFichierDTOBuilder;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.services.FichierService;
import com.minds.rgpd.business.utilities.StatusFichierEnum;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FichierServiceImpl implements FichierService {

    public static final String REGEX_FILENAME = "^([^_]+)_([^_]+)_Registre RGPD_ed([^.]+)\\.[^.]+\\.[a-z]+$";
    private final TraitementRepository traitementRepository;
    private final TraitementMapper traitementMapper;
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final EtablissementRepository etablissementRepository;
    private final EtablissementMapper etablissementMapper;

    @Override
    public InfoFichierDTOBuilder importFichier(MultipartFile fichier, InfoFichierDTOBuilder infoFichier) throws IOException {

        Pattern pattern = Pattern.compile(REGEX_FILENAME);
        String originalFilename = fichier.getOriginalFilename();
        Matcher matcher = pattern.matcher(Objects.requireNonNull(originalFilename));
        if (!matcher.matches()) {
            throw new IOException("Le fichier n'a pas le nom formaté comme attendu.");
        }
        String nomClient = matcher.group(1);
        String version = matcher.group(3);

        Client client = clientRepository.findByNom(nomClient);
        ClientDTO clientDTO = clientMapper.map(client);

        Sheet sheet = extractSheet(fichier);
        List<TraitementDTO> traitementDTOList = extractData(sheet)
                .stream()
                .map(cellules -> {
                    List<EtablissementDTO> etablissements = retrouverEtablissements(cellules, clientDTO);
                    return RowFileToTraitement.map(cellules, etablissements, clientDTO, Integer.parseInt(version));
                })
                .toList();
        List<Traitement> traitements = traitementMapper.mapToTraitementList(traitementDTOList);
        traitementRepository.saveAll(traitements);
        return infoFichier.dateFinTraitement(LocalDateTime.now()).statusFichier(StatusFichierEnum.OK);
    }

    private List<List<String>> extractData(Sheet sheet) {
        Iterable<Row> iterable = sheet::rowIterator;
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(row -> {
                    List<String> cellArray = new ArrayList<>();
                    if (row.getRowNum() >= 6 && allMandatoryValuePresent(row)) {
                        for (Cell cell : row) {
                            if (cell.getColumnIndex() >= 1 && cell.getColumnIndex() <= 38) {
                                String cellValue = getCellValue(cell);
                                cellArray.add(cellValue);
                            }
                        }
                        return cellArray;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean allMandatoryValuePresent(Row row) {
        return !getCellValue(row.getCell(1)).isBlank() && !getCellValue(row.getCell(4)).isBlank() && !getCellValue(row.getCell(5)).isBlank();
    }

    private Sheet extractSheet(MultipartFile fichier) throws IOException {
        String contentType = fichier.getContentType();
        if (Objects.isNull(contentType) || contentType.isEmpty()) {
            throw new IllegalArgumentException("Le contenu du fichier est vide ou null");
        }
        Workbook workbook = null;
        Sheet sheet;

        try (InputStream inputStream = fichier.getInputStream()) {
            String fileName = fichier.getOriginalFilename();
            assert fileName != null;
            if (fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else if (fileName.endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else {
                throw new IllegalArgumentException("Fichier Excel non supporté : " + fileName);
            }

            sheet = workbook.getSheetAt(1);
        } finally {
            if (Objects.nonNull(workbook)) {
                workbook.close();
            }
        }
        return sheet;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    // Format numbers to avoid scientific notation issues
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private List<EtablissementDTO> retrouverEtablissements(List<String> cellules, ClientDTO client) {
        String etablissements = cellules.get(1);
        return etablissements.lines()
                .filter(line -> !line.isBlank())
                .map(nom -> {
                    EtablissementDTO etablissement = EtablissementDTO.builder().id(UUID.randomUUID()).nom(nom).client(client).build();
                    return etablissementRepository.findByNom(nom)
                            .orElseGet(() -> etablissementRepository.save(etablissementMapper.map(etablissement)));
                })
                .map(etablissementMapper::map)
                .toList();
    }
}
