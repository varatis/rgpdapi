package com.minds.rgpd.business.dtos;

import lombok.AccessLevel;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder(access = AccessLevel.PUBLIC)
public record InfoFichierDTO(
        String nomFichier,
        LocalDateTime dateReception,
        LocalDateTime dateFinTraitement,
        String statusFichier,

        /**
         * RG3 : vrai lorsque l'import a été refusé faute de confirmation, le
         * registre du client contenant déjà des données. L'interface doit alors
         * afficher la modale d'avertissement puis rejouer l'appel avec
         * {@code confirmerRemplacement=true}.
         */
        boolean confirmationRequise,

        /** Aperçu des conséquences de l'import, renseigné avec {@link #confirmationRequise()}. */
        ImportApercuDTO apercu,

        /** RG4 : version du registre après import, telle que lue dans le fichier. */
        String version,

        /** Nombre de traitements supprimés par le remplacement (RG2). */
        Integer nombreTraitementsRemplaces,

        /** Nombre de traitements présents dans le registre à l'issue de l'import. */
        Integer nombreTraitementsImportes
) {
}
