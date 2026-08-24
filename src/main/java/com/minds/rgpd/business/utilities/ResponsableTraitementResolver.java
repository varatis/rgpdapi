package com.minds.rgpd.business.utilities;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.ResponsableTraitement;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ResponsableTraitementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Rattache a un traitement le responsable de traitement persiste correspondant.
 * <p>
 * Meme principe que {@link DefinitionResolver} et {@link DureeResolver} : un
 * responsable deja enregistre pour le client (meme valeur) est reutilise, sinon
 * il est cree. Les informations complementaires ne figurent pas dans le fichier
 * importe et restent nulles jusqu'a leur saisie via l'API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponsableTraitementResolver {

    private final ResponsableTraitementRepository responsableTraitementRepository;

    /**
     * Remplace le responsable transitoire du traitement par l'entite persistee
     * equivalente, en la creant au besoin.
     */
    public void resolveResponsableTraitement(Traitement traitement, Client client) {
        traitement.setResponsableTraitement(resolve(traitement.getResponsableTraitement(), client));
    }

    private ResponsableTraitement resolve(ResponsableTraitement source, Client client) {
        if (Objects.isNull(source) || StringUtils.isBlank(source.getValeur())) {
            return null;
        }
        String valeur = source.getValeur().strip();

        return responsableTraitementRepository.findByClientAndValeur(client, valeur)
                .orElseGet(() -> {
                    log.debug("Creation du responsable de traitement '{}' pour le client {}", valeur, client.getId());
                    return responsableTraitementRepository.save(ResponsableTraitement.builder()
                            .valeur(valeur)
                            .informationsComplementaires(source.getInformationsComplementaires())
                            .client(client)
                            .build());
                });
    }
}
