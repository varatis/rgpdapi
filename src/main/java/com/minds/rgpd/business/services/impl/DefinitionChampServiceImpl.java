package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.DefinitionChampDTO;
import com.minds.rgpd.business.services.DefinitionChampService;
import com.minds.rgpd.business.utilities.mappers.DefinitionChampMapper;
import com.minds.rgpd.persistence.entities.DefinitionChamp;
import com.minds.rgpd.persistence.repositories.DefinitionChampRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefinitionChampServiceImpl implements DefinitionChampService {

    private final DefinitionChampRepository definitionChampRepository;
    private final DefinitionChampMapper definitionChampMapper;

    @Override
    public List<DefinitionChampDTO> getDefinitions(String clientNom, String edition) {
        List<DefinitionChamp> definitions = (edition == null || edition.isBlank())
                ? definitionChampRepository.findByClientNomOrderByOrdreAsc(clientNom)
                : definitionChampRepository.findByClientNomAndEditionOrderByOrdreAsc(clientNom, edition);
        return definitionChampMapper.mapToDTOList(definitions);
    }
}
