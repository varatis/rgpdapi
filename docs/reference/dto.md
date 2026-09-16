# Objets échangés et critères de recherche

[Accueil](../index.md) / Références

> Référence extraite des sources par `tools/generate_docs_reference.py`. À relire après chaque évolution ; le code et le contrat runtime priment.

## Usage

Ces déclarations montrent les noms, types et contraintes des objets métier échangés. Les `FilterCriteria` modélisent des paramètres de recherche, pas des corps JSON. Les annotations de validation ne sont appliquées à une requête que si son parcours déclenche la validation (par exemple `@Valid`). Ne pas supposer que chaque DTO est accepté en écriture. Les propriétés calculées de classes complètes restent dans les sources.

Commencer par [API et exemples](../guides/04-api.md), puis vérifier le schéma exact dans `/v3/api-docs` de la version exécutée.

## ClientDTO

Source : [ClientDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ClientDTO.java).

```java
public record ClientDTO(
        UUID id,
        String nom,
        String statut,
        String version,
        LocalDate dateVersion,
        List<DureeDTO> durees,
        List<DefinitionDTO> definitions,
        List<ResponsableTraitementDTO> responsablesTraitement
)
```

## ClientLogoContentDTO

Source : [ClientLogoContentDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ClientLogoContentDTO.java).

```java
public record ClientLogoContentDTO(
        byte[] content,
        String contentType,
        String etag
)
```

## ClientLogoInfoDTO

Source : [ClientLogoInfoDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ClientLogoInfoDTO.java).

```java
public record ClientLogoInfoDTO(
        String nomFichier,
        Integer taille,
        String contentType,
        String etag
)
```

## ClientWriteDTO

Source : [ClientWriteDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ClientWriteDTO.java).

```java
public record ClientWriteDTO(
        @NotBlank(message = "est obligatoire")
        @Size(max = 255, message = "ne doit pas dépasser 255 caractères")
        String nom,

        @Size(max = 100, message = "ne doit pas dépasser 100 caractères")
        String statut,

        @Size(max = 20, message = "ne doit pas dépasser 20 caractères")
        String version,

        LocalDate dateVersion
)
```

## DefinitionDTO

Source : [DefinitionDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/DefinitionDTO.java).

```java
public record DefinitionDTO(
    Integer id,
    String type,
    String valeur
)
```

## DemandeDTO

Source : [DemandeDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/DemandeDTO.java).

```java
public record DemandeDTO(

        UUID id,

        String typeDemande,

        String descriptionSynthetique,

        String origine,

        LocalDate dateReception,

        String servicesConcernes,

        String detailTraitement,

        String servicesImpliques,

        String reponse,

        String alerteRt,

        DemandeStatut statut,

        UUID clientId

)
```

## DureeDTO

Source : [DureeDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/DureeDTO.java).

```java
public record DureeDTO(
    Integer id,
    boolean estArchivage,
    String valeur
)
```

## EtablissementDTO

Source : [EtablissementDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/EtablissementDTO.java).

```java
public record EtablissementDTO(
        UUID id,

        @NotBlank
        @Size(max = 255)
        String nom,

        // Trois caracteres : les codes DOM-TOM (971) et ceux de la Corse (2A, 2B).
        @Size(max = 3)
        String departement,

        boolean principal,

        @NotNull
        ClientDTO client
)
```

## EtablissementFilterCriteria

Source : [EtablissementFilterCriteria.java](../../src/main/java/com/minds/rgpd/business/dtos/EtablissementFilterCriteria.java).

```java
public record EtablissementFilterCriteria(
        String nom,
        String departement,
        Boolean principal
)
```

## HistorisationCreationDTO

Source : [HistorisationCreationDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/HistorisationCreationDTO.java).

```java
public record HistorisationCreationDTO(
        @NotBlank
        @Size(max = 2000)
        String motif,
        LocalDateTime date
)
```

## HistorisationDTO

Source : [HistorisationDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/HistorisationDTO.java).

```java
public record HistorisationDTO(
        Integer id,
        LocalDateTime date,
        String motif,
        String auteur
)
```

## ImportApercuDTO

Source : [ImportApercuDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ImportApercuDTO.java).

```java
public record ImportApercuDTO(
        String nomFichier,
        String clientNom,
        boolean fichierValide,
        String messageErreur,
        String versionActuelle,
        LocalDate dateVersionActuelle,
        String versionFichier,
        boolean remplacementDonnees,
        long nombreTraitementsExistants,
        long nombrePreconisationsExistantes,
        long nombreViolationsExistantes,
        String avertissement,
        String urlExportPrealable
)
```

## InfoFichierDTO

Source : [InfoFichierDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/InfoFichierDTO.java).

```java
public record InfoFichierDTO(
        String nomFichier,
        LocalDateTime dateReception,
        LocalDateTime dateFinTraitement,
        String statusFichier,

        
        boolean confirmationRequise,

        
        ImportApercuDTO apercu,

        
        String version,

        
        Integer nombreTraitementsRemplaces,

        
        Integer nombreTraitementsImportes
)
```

## PreconisationDTO

Source : [PreconisationDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/PreconisationDTO.java).

```java
public record PreconisationDTO(
        @NotNull
        UUID identifiant,
        @NotNull
        String libelle,
        String explication,
        String risqueEncours,
        String contraintes,
        String cout,
        String priorite,
        String complexite,
        String commentaire,
        String etatAvancement,
        ClientDTO client,
        UUID traitementIdentifiant,
        Integer traitementIdFonctionnel,
        String traitementNom
)
```

## PreconisationFilterCriteria

Source : [PreconisationFilterCriteria.java](../../src/main/java/com/minds/rgpd/business/dtos/PreconisationFilterCriteria.java).

```java
public record PreconisationFilterCriteria(
        String libelle,
        String etatAvancement,
        UUID idTraitement
)
```

## PreconisationPartielDTO

Source : [PreconisationPartielDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/PreconisationPartielDTO.java).

```java
public record PreconisationPartielDTO(
        UUID identifiant,
        String libelle,
        String priorite,
        String complexite,
        String etatAvancement,
        UUID traitementIdentifiant,
        String traitementNom
)
```

## ProfilDTO

Source : [ProfilDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ProfilDTO.java).

```java
public record ProfilDTO(
        UUID id,
        String code,
        String description
)
```

## ResponsableTraitementDTO

Source : [ResponsableTraitementDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ResponsableTraitementDTO.java).

```java
public record ResponsableTraitementDTO(
    Integer id,
    String valeur,
    String informationsComplementaires
)
```

## TraitementDTO

Source : [TraitementDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/TraitementDTO.java).

```java
public record TraitementDTO(

        @NotNull
        UUID identifiant,

        @NotNull
        Integer idFonctionnel,

        @NotNull
        String nom,

        String donneesConcernees,

        List<EtablissementDTO> etablissements,

        DefinitionDTO finalitePrincipale,

        ClientDTO client,

        Integer version,

        @NotNull
        LocalDate dateIdentification,

        LocalDate dateMiseAJour,

        String historiqueModifications,

        String dataProtectionOfficer,

        ResponsableTraitementDTO responsableTraitement,

        String gestionnaireMiseEnOeuvre,

        String sousFinalites,

        String categoriesPersonnesConcernees,

        String donneesIdentification,

        String donneesConnexion,

        String donneesLocalisation,

        String donneesComportementViePerso,

        String donneesEconomiquesFinancieres,

        String donneesProfessionnelles,

        String categoriesParticulieresDonnees,

        DefinitionDTO sensibilite,

        DefinitionDTO etudeImpact,

        String canauxCollecteDonnees,

        DefinitionDTO licieteTraitement,

        Boolean recoursTraitementAutomatises,

        String emplacementPhysique,

        String dispositionsSecuriteDonneesPhysique,

        String emplacementNumerique,

        String dispositionsSecuriteDonneesNumerique,

        String hebergement,

        DureeDTO dureeConservation,

        Boolean archivage,

        DureeDTO dureeArchivage,

        String categoriesDestinataires,

        String raisonsTransfertDestinataires,

        Boolean transfertsHorsUE,

        String paysDestinataires,

        String commentaires,

        Integer impactTraitement,

        Integer detournementFinalite,

        Integer scoreDetournementFinalite,

        Integer collecteDcpInappropriees,

        Integer scoreCollecteDcpInappropriees,

        Integer conservationExcessiveDcp,

        Integer scoreConservationExcessiveDcp,

        Integer securisationInsuffisanteDcp,

        Integer scoreSecurisationInsuffisanteDcp,

        Integer vicesConsentement,

        Integer scoreVicesConsentement,

        Integer manqueTransparence,

        Integer scoreManqueTransparence,

        Integer incapaciteExerciceDroits,

        Integer scoreIncapaciteExerciceDroits,

        Integer transfertTiersMalEncadre,

        Integer scoreTransfertTiersMalEncadre,

        Integer transfertHorsUeAbusif,

        Integer scoreTransfertHorsUeAbusif,

        Integer defautPreuve,

        Integer scoreDefautPreuve,

        Integer scoreGlobal,

        String commentairesAnalyse,

        Integer expositionTraitement,

        Boolean critereEvaluationScoring,

        Boolean critereDecisionAutomatique,

        Boolean critereSurveillanceSystematique,

        Boolean critereCollecteDonneesSensibles,

        Boolean critereCollecteLargeEchelle,

        Boolean critereCroisementDonnees,

        Boolean criterePersonnesVulnerables,

        Boolean critereUsageInnovant,

        Boolean critereExclusionBeneficeDroit,

        List<HistorisationDTO> historiqueTraitement
)
```

## TraitementFilterCriteria

Source : [TraitementFilterCriteria.java](../../src/main/java/com/minds/rgpd/business/dtos/TraitementFilterCriteria.java).

```java
public record TraitementFilterCriteria(
        String nom,
        String gestionnaireMiseEnOeuvre,
        String finalitePrincipale
)
```

## TraitementPartielDTO

Source : [TraitementPartielDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/TraitementPartielDTO.java).

```java
public record TraitementPartielDTO(
        UUID identifiant,
        Integer idFonctionnel,
        String nom,
        String gestionnaireMiseEnOeuvre,
        String finalitePrincipale
)
```

## UtilisateurDTO

Source : [UtilisateurDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/UtilisateurDTO.java).

```java
public record UtilisateurDTO(
        UUID id,
        String identifiant,
        String prenom,
        String nom,
        String email,
        boolean actif,
        List<String> roles,
        UUID clientId,
        String clientNom
)
```

## UtilisateurFilterCriteria

Source : [UtilisateurFilterCriteria.java](../../src/main/java/com/minds/rgpd/business/dtos/UtilisateurFilterCriteria.java).

```java
public record UtilisateurFilterCriteria(
        String nom,
        String prenom,
        UUID clientId
)
```

## UtilisateurWriteDTO

Source : [UtilisateurWriteDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/UtilisateurWriteDTO.java).

```java
public record UtilisateurWriteDTO(
        @NotBlank(message = "est obligatoire")
        @Size(max = 100, message = "ne doit pas dépasser 100 caractères")
        String prenom,
        @NotBlank(message = "est obligatoire")
        @Size(max = 100, message = "ne doit pas dépasser 100 caractères")
        String nom,
        @NotBlank(message = "est obligatoire")
        @Email(message = "doit être une adresse email valide")
        @Size(max = 255, message = "ne doit pas dépasser 255 caractères")
        String email,
        @NotEmpty(message = "doit contenir au moins un rôle")
        List<String> roles,
        UUID clientId,
        String groupe,
        Boolean actif,

        
        @Size(min = 8, max = 128, message = "doit contenir entre 8 et 128 caractères")
        String motDePasse
)
```

## ViolationDTO

Source : [ViolationDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ViolationDTO.java).

```java
public record ViolationDTO(

        UUID identifiant,

        @NotNull
        ClientDTO client,

        LocalDate dateViolation,

        String natureViolation,

        String donneesConcernees,

        Integer nombreApproximatifDonneesConcernees,

        String categoriesPersonnesConcernees,

        Integer nombrePersonnesConcernees,

        String consequences,

        String mesuresPrisesPrevues,

        String informationCnil,

        Boolean risqueEleveDroitsLibertes,

        String communicationPersonnesEffectueeEtDate,

        String commentaires,

        ViolationStatut statut
)
```

## ViolationFilterCriteria

Source : [ViolationFilterCriteria.java](../../src/main/java/com/minds/rgpd/business/dtos/ViolationFilterCriteria.java).

```java
public record ViolationFilterCriteria(
        String natureViolation,
        String donneesConcernees,
        Boolean risqueEleveDroitsLibertes,
        ViolationStatut statut,
        LocalDate dateViolationDebut,
        LocalDate dateViolationFin,
        Integer nombrePersonnesConcerneesMin,
        Integer nombrePersonnesConcerneesMax
)
```

## ViolationPartielDTO

Source : [ViolationPartielDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ViolationPartielDTO.java).

```java
public record ViolationPartielDTO(
        UUID identifiant,
        LocalDate dateViolation,
        String natureViolation,
        String donneesConcernees,
        Integer nombrePersonnesConcernees,
        Boolean risqueEleveDroitsLibertes,
        ViolationStatut statut
)
```
