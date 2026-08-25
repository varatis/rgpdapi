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
        traitement.setFinalitePrincipale(resolveFinalitePrincipale(valeurDe(traitement.getFinalitePrincipale()), client));
        traitement.setSensibilite(resolveSensibilite(valeurDe(traitement.getSensibilite()), client));
        traitement.setEtudeImpact(resolveEtudeImpact(valeurDe(traitement.getEtudeImpact()), client));
        traitement.setLicieteTraitement(resolveLiceiteTraitement(valeurDe(traitement.getLicieteTraitement()), client));
    }

    /** Definition de type {@link FinalitePrincipale} portant cette valeur, creee si besoin. */
    public FinalitePrincipale resolveFinalitePrincipale(String valeur, Client client) {
        return resolve(valeur, client, FinalitePrincipale.TYPE, FinalitePrincipale::new, FinalitePrincipale.class);
    }

    /** Definition de type {@link Sensibilite} portant cette valeur, creee si besoin. */
    public Sensibilite resolveSensibilite(String valeur, Client client) {
        return resolve(valeur, client, Sensibilite.TYPE, Sensibilite::new, Sensibilite.class);
    }

    /** Definition de type {@link EtudeImpact} portant cette valeur, creee si besoin. */
    public EtudeImpact resolveEtudeImpact(String valeur, Client client) {
        return resolve(valeur, client, EtudeImpact.TYPE, EtudeImpact::new, EtudeImpact.class);
    }

    /** Definition de type {@link LiceiteTraitement} portant cette valeur, creee si besoin. */
    public LiceiteTraitement resolveLiceiteTraitement(String valeur, Client client) {
        return resolve(valeur, client, LiceiteTraitement.TYPE, LiceiteTraitement::new, LiceiteTraitement.class);
    }

    private <T extends Definition> T resolve(
            String valeurBrute,
            Client client,
            String type,
            Supplier<T> factory,
            Class<T> concreteType
    ) {
        if (StringUtils.isBlank(valeurBrute)) {
            return null;
        }
        String valeur = valeurBrute.strip();

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

    private static String valeurDe(Definition definition) {
        return Objects.isNull(definition) ? null : definition.getValeur();
    }
}
