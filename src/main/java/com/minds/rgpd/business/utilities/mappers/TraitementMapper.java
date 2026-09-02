package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.DefinitionDTO;
import com.minds.rgpd.business.dtos.HistorisationDTO;
import com.minds.rgpd.business.dtos.DureeDTO;
import com.minds.rgpd.business.dtos.ResponsableTraitementDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.minds.rgpd.persistence.entities.Definition;
import com.minds.rgpd.persistence.entities.Duree;
import com.minds.rgpd.persistence.entities.EtudeImpact;
import com.minds.rgpd.persistence.entities.HistorisationTraitement;
import com.minds.rgpd.persistence.entities.FinalitePrincipale;
import com.minds.rgpd.persistence.entities.LiceiteTraitement;
import com.minds.rgpd.persistence.entities.ResponsableTraitement;
import com.minds.rgpd.persistence.entities.Sensibilite;
import com.minds.rgpd.persistence.entities.Traitement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring", uses = ClientRefMapper.class)
public interface TraitementMapper {

    TraitementDTO mapToDTO(Traitement traitement);

    default Page<TraitementPartielDTO> toTraitementPartielDTOPage(Page<Traitement> traitementsPage) {
        if (Objects.isNull(traitementsPage)) {
            return null;
        }
        List<TraitementPartielDTO> partielDTOs = mapToPartialDTOList(traitementsPage.getContent());
        return new PageImpl<>(partielDTOs, traitementsPage.getPageable(), traitementsPage.getTotalElements());
    }

    List<TraitementPartielDTO> mapToPartialDTOList(List<Traitement> traitements);

    TraitementPartielDTO map(Traitement traitement);

    List<TraitementDTO> mapToDTOList(List<Traitement> traitements);

    @Mapping(target = "etablissements", ignore = true)
    @Mapping(target = "historiqueTraitement", ignore = true)
    Traitement mapToTraitement(TraitementDTO traitementDTO);

    List<Traitement> mapToTraitementList(List<TraitementDTO> traitementsDTO);

    /**
     * Les références vers le référentiel du client sont ignorées ici et
     * réaffectées par {@link #copierReferentiels}. Sans cela, MapStruct
     * réutiliserait l'entité déjà rattachée au traitement et écrirait dessus :
     * la définition étant partagée entre traitements, modifier un traitement
     * en renommerait la valeur pour tous les autres.
     */
    @Mapping(target = "identifiant", ignore = true)
    @Mapping(target = "idFonctionnel", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "etablissements", ignore = true)
    @Mapping(target = "finalitePrincipale", ignore = true)
    @Mapping(target = "sensibilite", ignore = true)
    @Mapping(target = "etudeImpact", ignore = true)
    @Mapping(target = "licieteTraitement", ignore = true)
    @Mapping(target = "dureeConservation", ignore = true)
    @Mapping(target = "dureeArchivage", ignore = true)
    @Mapping(target = "responsableTraitement", ignore = true)
    @Mapping(target = "historiqueTraitement", ignore = true)
    void updateTraitementFromDto(TraitementDTO traitementDTO, @MappingTarget Traitement traitement);

    /**
     * Reporte sur {@code traitement} les références vers le référentiel portées
     * par {@code source}.
     * <p>
     * Utilisé à la modification : les résolveurs travaillent sur un traitement
     * transitoire issu du DTO, et seules les entités persistées qu'ils en
     * retirent sont rattachées au traitement managé. Les y rattacher avant
     * résolution ferait échouer le premier flush déclenché par les résolveurs,
     * le traitement managé pointant alors des instances transitoires.
     */
    default void copierReferentiels(Traitement source, @MappingTarget Traitement traitement) {
        traitement.setFinalitePrincipale(source.getFinalitePrincipale());
        traitement.setSensibilite(source.getSensibilite());
        traitement.setEtudeImpact(source.getEtudeImpact());
        traitement.setLicieteTraitement(source.getLicieteTraitement());
        traitement.setDureeConservation(source.getDureeConservation());
        traitement.setDureeArchivage(source.getDureeArchivage());
        traitement.setResponsableTraitement(source.getResponsableTraitement());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    FinalitePrincipale toFinalitePrincipale(DefinitionDTO definitionDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    Sensibilite toSensibilite(DefinitionDTO definitionDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    EtudeImpact toEtudeImpact(DefinitionDTO definitionDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    LiceiteTraitement toLiceiteTraitement(DefinitionDTO definitionDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    Duree toDuree(DureeDTO dureeDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    ResponsableTraitement toResponsableTraitement(ResponsableTraitementDTO responsableTraitementDTO);

    /**
     * L'historique est exposé en lecture seule : il n'est alimenté que par
     * {@link com.minds.rgpd.business.services.HistorisationService}, jamais par
     * le corps d'une requête de modification de traitement.
     */
    HistorisationDTO mapHistorisation(HistorisationTraitement historisation);

    List<HistorisationDTO> mapHistorisations(List<HistorisationTraitement> historisations);

    /**
     * Aplatit une définition sur sa seule valeur textuelle, pour les vues
     * résumées telles que {@link TraitementPartielDTO}.
     */
    default String definitionToValeur(Definition definition) {
        return Objects.isNull(definition) ? null : definition.getValeur();
    }

}
