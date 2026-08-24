package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.DefinitionDTO;
import com.minds.rgpd.business.dtos.DureeDTO;
import com.minds.rgpd.business.dtos.ResponsableTraitementDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Duree;
import com.minds.rgpd.persistence.entities.FinalitePrincipale;
import com.minds.rgpd.persistence.entities.ResponsableTraitement;
import com.minds.rgpd.persistence.entities.Traitement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Les entites referencent leur client, et le client reference ces mêmes
 * entites : depuis que les noms de champs sont alignes entre entites et DTOs,
 * MapStruct mappe les deux sens et bouclerait indefiniment si la reference
 * retour etait exposee. Deux garde-fous l'evitent : les DTOs enfants ne
 * portent pas de client, et {@link ClientRefMapper} reduit un client imbrique
 * a sa seule identite. Ces tests garantissent qu'ils restent en place.
 */
class MappingCyclesTest {

    private final ClientMapper clientMapper = new ClientMapperImpl();
    private TraitementMapper traitementMapper;

    private Client client;

    @BeforeEach
    void setUp() {
        traitementMapper = new TraitementMapperImpl();
        // TraitementMapperImpl s'appuie sur ClientRefMapper, injecte par Spring en temps normal
        var champ = java.util.Arrays.stream(TraitementMapperImpl.class.getDeclaredFields())
                .filter(f -> f.getType().equals(ClientRefMapper.class))
                .findFirst()
                .orElseThrow();
        champ.setAccessible(true);
        try {
            champ.set(traitementMapper, new ClientRefMapperImpl());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        client = Client.builder().id(UUID.randomUUID()).nom("Dupont").statut("ACTIF").build();
        client.setResponsablesTraitement(List.of(
                ResponsableTraitement.builder().id(1).valeur("DSI").client(client).build()));
        client.setDurees(List.of(
                Duree.builder().id(1).valeur("5 ans").estArchivage(Duree.CONSERVATION).client(client).build()));
        client.setDefinitions(List.of(
                FinalitePrincipale.builder().id(1).valeur("Gestion de la paie").client(client).build()));
    }

    @Test
    void mappeUnClientPorteurDeSesCollectionsSansBoucler() {
        ClientDTO dto = clientMapper.map(client);

        assertEquals(1, dto.responsablesTraitement().size());
        assertEquals(1, dto.durees().size());
        assertEquals(1, dto.definitions().size());
        // La reference retour n'existe pas : la retablir rouvrirait le cycle
        assertAucunClient(DureeDTO.class);
        assertAucunClient(ResponsableTraitementDTO.class);
        assertAucunClient(DefinitionDTO.class);
    }

    private void assertAucunClient(Class<? extends Record> dto) {
        boolean porteUnClient = Arrays.stream(dto.getRecordComponents())
                .anyMatch(composant -> "client".equals(composant.getName()));
        assertFalse(porteUnClient, dto.getSimpleName() + " ne doit pas referencer son client");
    }

    @Test
    void mappeUnTraitementSansBouclerSurSonClient() {
        Traitement traitement = Traitement.builder()
                .identifiant(UUID.randomUUID())
                .nom("Paie")
                .client(client)
                .responsableTraitement(client.getResponsablesTraitement().getFirst())
                .dureeConservation(client.getDurees().getFirst())
                .build();

        TraitementDTO dto = traitementMapper.mapToDTO(traitement);

        assertEquals("Dupont", dto.client().nom());
        // Le client imbrique reste reduit a son identite
        assertNull(dto.client().durees());
        assertNull(dto.client().responsablesTraitement());
        assertNull(dto.client().definitions());
        assertNotNull(dto.responsableTraitement());
        assertEquals("5 ans", dto.dureeConservation().valeur());
    }
}
