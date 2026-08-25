package com.minds.rgpd.business.utilities;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Duree;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.DureeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Rattache a un traitement les durees persistees correspondantes.
 * <p>
 * Meme principe que {@link DefinitionResolver} : une duree deja enregistree
 * pour le client (meme nature, meme valeur) est reutilisee, sinon elle est
 * creee. Sans cela, les durees issues d'un import seraient transitoires et la
 * sauvegarde du traitement echouerait, ou bien la meme valeur serait dupliquee
 * a chaque ligne importee.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DureeResolver {

    private final DureeRepository dureeRepository;

    /**
     * Remplace les durees transitoires du traitement par les entites
     * persistees equivalentes, en les creant au besoin.
     */
    public void resolveDurees(Traitement traitement, Client client) {
        traitement.setDureeConservation(resolveDureeConservation(valeurDe(traitement.getDureeConservation()), client));
        traitement.setDureeArchivage(resolveDureeArchivage(valeurDe(traitement.getDureeArchivage()), client));
    }

    /** Duree de conservation portant cette valeur, creee si besoin. */
    public Duree resolveDureeConservation(String valeur, Client client) {
        return resolve(valeur, client, Duree.CONSERVATION);
    }

    /** Duree d'archivage portant cette valeur, creee si besoin. */
    public Duree resolveDureeArchivage(String valeur, Client client) {
        return resolve(valeur, client, Duree.ARCHIVAGE);
    }

    private Duree resolve(String valeurBrute, Client client, boolean estArchivage) {
        if (StringUtils.isBlank(valeurBrute)) {
            return null;
        }
        String valeur = valeurBrute.strip();

        return dureeRepository.findByClientAndEstArchivageAndValeur(client, estArchivage, valeur)
                .orElseGet(() -> {
                    log.debug("Creation de la duree [estArchivage={}] '{}' pour le client {}",
                            estArchivage, valeur, client.getId());
                    return dureeRepository.save(Duree.builder()
                            .estArchivage(estArchivage)
                            .valeur(valeur)
                            .client(client)
                            .build());
                });
    }

    private static String valeurDe(Duree duree) {
        return Objects.isNull(duree) ? null : duree.getValeur();
    }
}
