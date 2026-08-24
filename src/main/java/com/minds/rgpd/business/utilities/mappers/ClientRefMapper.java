package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.persistence.entities.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mappe un client reduit a son identite, sans ses collections.
 * <p>
 * Utilise par tous les mappers qui imbriquent un client dans un autre objet
 * (traitement, etablissement, utilisateur). Deux raisons a cela : les elements
 * de ces collections referencent le client en retour, ce qui ferait boucler le
 * mapping indefiniment ; et un traitement n'a pas a transporter l'integralite
 * du referentiel de son client.
 * <p>
 * {@link ClientMapper} reste le mapper complet, pour les endpoints qui exposent
 * reellement un client et son referentiel.
 */
@Mapper(componentModel = "spring")
public interface ClientRefMapper {

    @Mapping(target = "durees", ignore = true)
    @Mapping(target = "definitions", ignore = true)
    @Mapping(target = "responsablesTraitement", ignore = true)
    ClientDTO map(Client client);

    @Mapping(target = "durees", ignore = true)
    @Mapping(target = "definitions", ignore = true)
    @Mapping(target = "responsablesTraitement", ignore = true)
    Client map(ClientDTO clientDTO);
}
