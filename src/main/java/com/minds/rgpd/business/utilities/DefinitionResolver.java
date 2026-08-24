package com.minds.rgpd.business.utilities;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Definition;
import com.minds.rgpd.persistence.entities.EtudeImpact;
import com.minds.rgpd.persistence.entities.FinalitePrincipale;
import com.minds.rgpd.persistence.entities.LiceiteTraitement;
import com.minds.rgpd.persistence.entities.Sensibilite;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.DefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Rattache a un traitement les definitions persistees correspondantes.
 * <p>
 * Une definition deja enregistree pour le client (meme type, meme valeur) est
 * reutilisee ; sinon elle est creee. Sans cela, les definitions issues d'un
 * import sont transitoires et la sauvegarde du traitement echouerait, ou bien
 * la meme valeur serait dupliquee a chaque ligne importee.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefinitionResolver {

    private final DefinitionRepository definitionRepository;

    /**
     * Remplace les definitions transitoires du traitement par les entites
     * persistees equivalentes, en les creant au besoin.
     */
    public void resolveDefinitions(Traitement traitement, Client client) {
        traitement.setFinalitePrincipale(resolve(traitement.getFinalitePrincipale(), client,
                FinalitePrincipale.TYPE, FinalitePrincipale::new, FinalitePrincipale.class));
        traitement.setSensibilite(resolve(traitement.getSensibilite(), client,
                Sensibilite.TYPE, Sensibilite::new, Sensibilite.class));
        traitement.setEtudeImpact(resolve(traitement.getEtudeImpact(), client,
                EtudeImpact.TYPE, EtudeImpact::new, EtudeImpact.class));
        traitement.setLicieteTraitement(resolve(traitement.getLicieteTraitement(), client,
                LiceiteTraitement.TYPE, LiceiteTraitement::new, LiceiteTraitement.class));
    }

    private <T extends Definition> T resolve(
            Definition source,
            Client client,
            String type,
            Supplier<T> factory,
            Class<T> concreteType
    ) {
        if (Objects.isNull(source) || StringUtils.isBlank(source.getValeur())) {
            return null;
        }
        String valeur = source.getValeur().strip();

        return definitionRepository.findByClientAndTypeAndValeur(client, type, valeur)
                .map(concreteType::cast)
                .orElseGet(() -> {
                    log.debug("Creation de la definition [{}] '{}' pour le client {}", type, valeur, client.getId());
                    T definition = factory.get();
                    definition.setValeur(valeur);
                    definition.setClient(client);
                    return definitionRepository.save(definition);
                });
    }
}
