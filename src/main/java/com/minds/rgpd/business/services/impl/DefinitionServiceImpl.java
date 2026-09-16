package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.services.DefinitionService;
import com.minds.rgpd.persistence.entities.EtudeImpact;
import com.minds.rgpd.persistence.entities.LiceiteTraitement;
import com.minds.rgpd.persistence.entities.Sensibilite;
import com.minds.rgpd.persistence.repositories.DefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefinitionServiceImpl implements DefinitionService {

    /** Clé d'URL → discriminateur de la définition. */
    private static final Map<String, String> TYPES = Map.of(
            "sensibilite", Sensibilite.TYPE,
            "etude-impact", EtudeImpact.TYPE,
            "liceite-traitement", LiceiteTraitement.TYPE
    );

    private final DefinitionRepository definitionRepository;

    @Override
    public List<String> getValues(String clientNom, String type) {
        String discriminator = TYPES.get(type);
        if (Objects.isNull(discriminator)) {
            throw new IllegalArgumentException("Référentiel inconnu : " + type);
        }
        return definitionRepository.findValuesByClientNomAndType(clientNom, discriminator);
    }
}
