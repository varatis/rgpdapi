package com.minds.rgpd.business.Imports;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.data.jpa.repository.JpaRepository;

public record ImportSpecification<T>(String sheetName, boolean allowEmpty, int headerRows, List<String> columns,
    Function<ExcelRow, T> mapper, Predicate<T> isDuplicate, JpaRepository<T, ?> repository) 
{

}