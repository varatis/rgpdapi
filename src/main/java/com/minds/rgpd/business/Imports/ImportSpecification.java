package com.minds.rgpd.business.Imports;

import java.util.List;
import java.util.function.Function;

/**
 * Décrit la lecture d'une feuille Excel : nom de la feuille, position de
 * l'en-tête, colonnes obligatoires et transformation d'une ligne en entité.
 * <p>
 * Depuis la règle RG2, l'import ne fusionne plus avec les données existantes :
 * il remplace l'état précédent du registre du client. Le filtrage des doublons
 * et la persistance générique qui accompagnaient l'ancienne logique ont donc
 * été retirés ; la persistance est pilotée par {@code FichierServiceImpl}.
 */
public record ImportSpecification<T>(
        String sheetName,
        boolean allowEmpty,
        int headerRows,
        List<String> columns,
        Function<ExcelRow, T> mapper)
{

}
