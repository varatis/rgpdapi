# Index navigable du code et des tests

[Accueil](../index.md) / Références

> Référence extraite des sources par `tools/generate_docs_reference.py`. À relire après chaque évolution ; le code et le contrat runtime priment.

## Où modifier le code ?

Les sources compilées sont sous `src/main/java`. Les copies racine `business/` et `infrastructure/` ne sont pas des racines Maven déclarées. Cet index couvre aussi tests, ressources et scripts pour faciliter la navigation.

## `.platforms/ci`

- [ZAP-securityScan-README.md](../../.platforms/ci/ZAP-securityScan-README.md)
- [bootstrap.sh](../../.platforms/ci/bootstrap.sh)
- [build.sh](../../.platforms/ci/build.sh)
- [check-licenses.sh](../../.platforms/ci/check-licenses.sh)
- [check-vulnerability.sh](../../.platforms/ci/check-vulnerability.sh)
- [docker-compose-test-package.yml](../../.platforms/ci/docker-compose-test-package.yml)
- [get-git-version.sh](../../.platforms/ci/get-git-version.sh)
- [git-version.sh](../../.platforms/ci/git-version.sh)
- [package.sh](../../.platforms/ci/package.sh)
- [run-test-build.sh](../../.platforms/ci/run-test-build.sh)
- [run-test-package.sh](../../.platforms/ci/run-test-package.sh)
- [security-scan.sh](../../.platforms/ci/security-scan.sh)
- [sonar.sh](../../.platforms/ci/sonar.sh)
- [trivy.sh](../../.platforms/ci/trivy.sh)
- [zap.sh](../../.platforms/ci/zap.sh)

## `.platforms/ci/docker-compose`

- [docker-compose-test-build.yml](../../.platforms/ci/docker-compose/docker-compose-test-build.yml)

## `.platforms/ci/dockerfiles`

- [dockerfile](../../.platforms/ci/dockerfiles/dockerfile)
- [gitlab-dockerfile](../../.platforms/ci/dockerfiles/gitlab-dockerfile)

## `.platforms/k8s`

- [bootstrap-k8s-interne.sh](../../.platforms/k8s/bootstrap-k8s-interne.sh)
- [bootstrap-k8s-minds.sh](../../.platforms/k8s/bootstrap-k8s-minds.sh)
- [deploy.sh](../../.platforms/k8s/deploy.sh)
- [undeploy.sh](../../.platforms/k8s/undeploy.sh)
- [values-demo.yaml](../../.platforms/k8s/values-demo.yaml)
- [values-int.yaml](../../.platforms/k8s/values-int.yaml)
- [values-prod.yaml](../../.platforms/k8s/values-prod.yaml)
- [values-valid.yaml](../../.platforms/k8s/values-valid.yaml)

## `.platforms/k8s/helm`

- [.helmignore](../../.platforms/k8s/helm/.helmignore)
- [Chart.yaml](../../.platforms/k8s/helm/Chart.yaml)
- [values.yaml](../../.platforms/k8s/helm/values.yaml)

## `.platforms/k8s/helm/templates`

- [configMap.yaml](../../.platforms/k8s/helm/templates/configMap.yaml)
- [deployment.yaml](../../.platforms/k8s/helm/templates/deployment.yaml)
- [ingress.yaml](../../.platforms/k8s/helm/templates/ingress.yaml)
- [secret.yaml](../../.platforms/k8s/helm/templates/secret.yaml)
- [service.yaml](../../.platforms/k8s/helm/templates/service.yaml)

## `business/identity`

- [GroupeIdentite.java](../../business/identity/GroupeIdentite.java)
- [IdentiteCommande.java](../../business/identity/IdentiteCommande.java)
- [IdentiteUtilisateur.java](../../business/identity/IdentiteUtilisateur.java)
- [IdentityGateway.java](../../business/identity/IdentityGateway.java)

## `infrastructure/keycloak`

- [KeycloakAdminClient.java](../../infrastructure/keycloak/KeycloakAdminClient.java)
- [KeycloakClientRepresentation.java](../../infrastructure/keycloak/KeycloakClientRepresentation.java)
- [KeycloakConfig.java](../../infrastructure/keycloak/KeycloakConfig.java)
- [KeycloakGroupRepresentation.java](../../infrastructure/keycloak/KeycloakGroupRepresentation.java)
- [KeycloakIdentityGateway.java](../../infrastructure/keycloak/KeycloakIdentityGateway.java)
- [KeycloakProperties.java](../../infrastructure/keycloak/KeycloakProperties.java)
- [KeycloakRoleRepresentation.java](../../infrastructure/keycloak/KeycloakRoleRepresentation.java)
- [KeycloakTokenResponse.java](../../infrastructure/keycloak/KeycloakTokenResponse.java)
- [KeycloakUserRepresentation.java](../../infrastructure/keycloak/KeycloakUserRepresentation.java)

## `src/main/java/com/minds/rgpd`

- [RgpdApplication.java](../../src/main/java/com/minds/rgpd/RgpdApplication.java)

## `src/main/java/com/minds/rgpd/business/Imports`

- [ExcelImportService.java](../../src/main/java/com/minds/rgpd/business/Imports/ExcelImportService.java)
- [ExcelParsingException.java](../../src/main/java/com/minds/rgpd/business/Imports/ExcelParsingException.java)
- [ExcelRow.java](../../src/main/java/com/minds/rgpd/business/Imports/ExcelRow.java)
- [ImportError.java](../../src/main/java/com/minds/rgpd/business/Imports/ImportError.java)
- [ImportResult.java](../../src/main/java/com/minds/rgpd/business/Imports/ImportResult.java)
- [ImportSpecification.java](../../src/main/java/com/minds/rgpd/business/Imports/ImportSpecification.java)
- [ImportSpecifications.java](../../src/main/java/com/minds/rgpd/business/Imports/ImportSpecifications.java)

## `src/main/java/com/minds/rgpd/business/dtos`

- [ClientDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ClientDTO.java)
- [ClientLogoContentDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ClientLogoContentDTO.java)
- [ClientLogoInfoDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ClientLogoInfoDTO.java)
- [ClientWriteDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ClientWriteDTO.java)
- [DefinitionDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/DefinitionDTO.java)
- [DemandeDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/DemandeDTO.java)
- [DureeDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/DureeDTO.java)
- [EtablissementDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/EtablissementDTO.java)
- [EtablissementFilterCriteria.java](../../src/main/java/com/minds/rgpd/business/dtos/EtablissementFilterCriteria.java)
- [HistorisationCreationDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/HistorisationCreationDTO.java)
- [HistorisationDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/HistorisationDTO.java)
- [ImportApercuDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ImportApercuDTO.java)
- [InfoFichierDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/InfoFichierDTO.java)
- [PreconisationDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/PreconisationDTO.java)
- [PreconisationFilterCriteria.java](../../src/main/java/com/minds/rgpd/business/dtos/PreconisationFilterCriteria.java)
- [PreconisationPartielDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/PreconisationPartielDTO.java)
- [ProfilDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ProfilDTO.java)
- [ResponsableTraitementDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ResponsableTraitementDTO.java)
- [TraitementDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/TraitementDTO.java)
- [TraitementFilterCriteria.java](../../src/main/java/com/minds/rgpd/business/dtos/TraitementFilterCriteria.java)
- [TraitementPartielDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/TraitementPartielDTO.java)
- [UtilisateurDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/UtilisateurDTO.java)
- [UtilisateurFilterCriteria.java](../../src/main/java/com/minds/rgpd/business/dtos/UtilisateurFilterCriteria.java)
- [UtilisateurWriteDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/UtilisateurWriteDTO.java)
- [ViolationDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ViolationDTO.java)
- [ViolationFilterCriteria.java](../../src/main/java/com/minds/rgpd/business/dtos/ViolationFilterCriteria.java)
- [ViolationPartielDTO.java](../../src/main/java/com/minds/rgpd/business/dtos/ViolationPartielDTO.java)

## `src/main/java/com/minds/rgpd/business/enums`

- [DemandeStatut.java](../../src/main/java/com/minds/rgpd/business/enums/DemandeStatut.java)
- [ViolationStatut.java](../../src/main/java/com/minds/rgpd/business/enums/ViolationStatut.java)

## `src/main/java/com/minds/rgpd/business/exceptions`

- [DuplicateResourceException.java](../../src/main/java/com/minds/rgpd/business/exceptions/DuplicateResourceException.java)
- [GlobalExceptionHandler.java](../../src/main/java/com/minds/rgpd/business/exceptions/GlobalExceptionHandler.java)
- [IdentityProviderException.java](../../src/main/java/com/minds/rgpd/business/exceptions/IdentityProviderException.java)
- [IdentityProviderIndisponibleException.java](../../src/main/java/com/minds/rgpd/business/exceptions/IdentityProviderIndisponibleException.java)
- [InvalidFileException.java](../../src/main/java/com/minds/rgpd/business/exceptions/InvalidFileException.java)
- [ResourceInUseException.java](../../src/main/java/com/minds/rgpd/business/exceptions/ResourceInUseException.java)
- [ResourceNotFoundException.java](../../src/main/java/com/minds/rgpd/business/exceptions/ResourceNotFoundException.java)

## `src/main/java/com/minds/rgpd/business/identity`

- [GroupeIdentite.java](../../src/main/java/com/minds/rgpd/business/identity/GroupeIdentite.java)
- [IdentiteCommande.java](../../src/main/java/com/minds/rgpd/business/identity/IdentiteCommande.java)
- [IdentiteUtilisateur.java](../../src/main/java/com/minds/rgpd/business/identity/IdentiteUtilisateur.java)
- [IdentityGateway.java](../../src/main/java/com/minds/rgpd/business/identity/IdentityGateway.java)

## `src/main/java/com/minds/rgpd/business/services`

- [ClientLogoService.java](../../src/main/java/com/minds/rgpd/business/services/ClientLogoService.java)
- [ClientService.java](../../src/main/java/com/minds/rgpd/business/services/ClientService.java)
- [DemandeService.java](../../src/main/java/com/minds/rgpd/business/services/DemandeService.java)
- [EtablissementService.java](../../src/main/java/com/minds/rgpd/business/services/EtablissementService.java)
- [FichierService.java](../../src/main/java/com/minds/rgpd/business/services/FichierService.java)
- [HistorisationService.java](../../src/main/java/com/minds/rgpd/business/services/HistorisationService.java)
- [PreconisationService.java](../../src/main/java/com/minds/rgpd/business/services/PreconisationService.java)
- [ProfilService.java](../../src/main/java/com/minds/rgpd/business/services/ProfilService.java)
- [TraitementService.java](../../src/main/java/com/minds/rgpd/business/services/TraitementService.java)
- [UtilisateurService.java](../../src/main/java/com/minds/rgpd/business/services/UtilisateurService.java)
- [ViolationService.java](../../src/main/java/com/minds/rgpd/business/services/ViolationService.java)

## `src/main/java/com/minds/rgpd/business/services/impl`

- [ClientLogoServiceImpl.java](../../src/main/java/com/minds/rgpd/business/services/impl/ClientLogoServiceImpl.java)
- [ClientServiceImpl.java](../../src/main/java/com/minds/rgpd/business/services/impl/ClientServiceImpl.java)
- [DemandeServiceImpl.java](../../src/main/java/com/minds/rgpd/business/services/impl/DemandeServiceImpl.java)
- [EtablissementServiceImpl.java](../../src/main/java/com/minds/rgpd/business/services/impl/EtablissementServiceImpl.java)
- [FichierServiceImpl.java](../../src/main/java/com/minds/rgpd/business/services/impl/FichierServiceImpl.java)
- [HistorisationServiceImpl.java](../../src/main/java/com/minds/rgpd/business/services/impl/HistorisationServiceImpl.java)
- [PreconisationServiceImpl.java](../../src/main/java/com/minds/rgpd/business/services/impl/PreconisationServiceImpl.java)
- [ProfilServiceImpl.java](../../src/main/java/com/minds/rgpd/business/services/impl/ProfilServiceImpl.java)
- [TraitementServiceImpl.java](../../src/main/java/com/minds/rgpd/business/services/impl/TraitementServiceImpl.java)
- [UtilisateurServiceImpl.java](../../src/main/java/com/minds/rgpd/business/services/impl/UtilisateurServiceImpl.java)
- [ViolationServiceImpl.java](../../src/main/java/com/minds/rgpd/business/services/impl/ViolationServiceImpl.java)

## `src/main/java/com/minds/rgpd/business/utilities`

- [AccessUserInformation.java](../../src/main/java/com/minds/rgpd/business/utilities/AccessUserInformation.java)
- [DefinitionResolver.java](../../src/main/java/com/minds/rgpd/business/utilities/DefinitionResolver.java)
- [DureeResolver.java](../../src/main/java/com/minds/rgpd/business/utilities/DureeResolver.java)
- [FormatImage.java](../../src/main/java/com/minds/rgpd/business/utilities/FormatImage.java)
- [ResponsableTraitementResolver.java](../../src/main/java/com/minds/rgpd/business/utilities/ResponsableTraitementResolver.java)
- [TraitementDiff.java](../../src/main/java/com/minds/rgpd/business/utilities/TraitementDiff.java)

## `src/main/java/com/minds/rgpd/business/utilities/mappers`

- [ClientMapper.java](../../src/main/java/com/minds/rgpd/business/utilities/mappers/ClientMapper.java)
- [ClientRefMapper.java](../../src/main/java/com/minds/rgpd/business/utilities/mappers/ClientRefMapper.java)
- [DemandeMapper.java](../../src/main/java/com/minds/rgpd/business/utilities/mappers/DemandeMapper.java)
- [EtablissementMapper.java](../../src/main/java/com/minds/rgpd/business/utilities/mappers/EtablissementMapper.java)
- [PreconisationMapper.java](../../src/main/java/com/minds/rgpd/business/utilities/mappers/PreconisationMapper.java)
- [ProfilMapper.java](../../src/main/java/com/minds/rgpd/business/utilities/mappers/ProfilMapper.java)
- [TraitementMapper.java](../../src/main/java/com/minds/rgpd/business/utilities/mappers/TraitementMapper.java)
- [ViolationMapper.java](../../src/main/java/com/minds/rgpd/business/utilities/mappers/ViolationMapper.java)

## `src/main/java/com/minds/rgpd/infrastructure/keycloak`

- [KeycloakAdminClient.java](../../src/main/java/com/minds/rgpd/infrastructure/keycloak/KeycloakAdminClient.java)
- [KeycloakClientRepresentation.java](../../src/main/java/com/minds/rgpd/infrastructure/keycloak/KeycloakClientRepresentation.java)
- [KeycloakConfig.java](../../src/main/java/com/minds/rgpd/infrastructure/keycloak/KeycloakConfig.java)
- [KeycloakGroupRepresentation.java](../../src/main/java/com/minds/rgpd/infrastructure/keycloak/KeycloakGroupRepresentation.java)
- [KeycloakIdentityGateway.java](../../src/main/java/com/minds/rgpd/infrastructure/keycloak/KeycloakIdentityGateway.java)
- [KeycloakProperties.java](../../src/main/java/com/minds/rgpd/infrastructure/keycloak/KeycloakProperties.java)
- [KeycloakRoleRepresentation.java](../../src/main/java/com/minds/rgpd/infrastructure/keycloak/KeycloakRoleRepresentation.java)
- [KeycloakTokenResponse.java](../../src/main/java/com/minds/rgpd/infrastructure/keycloak/KeycloakTokenResponse.java)
- [KeycloakUserRepresentation.java](../../src/main/java/com/minds/rgpd/infrastructure/keycloak/KeycloakUserRepresentation.java)

## `src/main/java/com/minds/rgpd/infrastructure/security`

- [CorsConfig.java](../../src/main/java/com/minds/rgpd/infrastructure/security/CorsConfig.java)
- [JwtAuthConverter.java](../../src/main/java/com/minds/rgpd/infrastructure/security/JwtAuthConverter.java)
- [OpenApiConfiguration.java](../../src/main/java/com/minds/rgpd/infrastructure/security/OpenApiConfiguration.java)
- [SecurityConfig.java](../../src/main/java/com/minds/rgpd/infrastructure/security/SecurityConfig.java)
- [SslConfig.java](../../src/main/java/com/minds/rgpd/infrastructure/security/SslConfig.java)

## `src/main/java/com/minds/rgpd/persistence/entities`

- [Client.java](../../src/main/java/com/minds/rgpd/persistence/entities/Client.java)
- [ClientLogo.java](../../src/main/java/com/minds/rgpd/persistence/entities/ClientLogo.java)
- [CurrentUser.java](../../src/main/java/com/minds/rgpd/persistence/entities/CurrentUser.java)
- [Definition.java](../../src/main/java/com/minds/rgpd/persistence/entities/Definition.java)
- [Demande.java](../../src/main/java/com/minds/rgpd/persistence/entities/Demande.java)
- [Duree.java](../../src/main/java/com/minds/rgpd/persistence/entities/Duree.java)
- [Etablissement.java](../../src/main/java/com/minds/rgpd/persistence/entities/Etablissement.java)
- [EtudeImpact.java](../../src/main/java/com/minds/rgpd/persistence/entities/EtudeImpact.java)
- [FinalitePrincipale.java](../../src/main/java/com/minds/rgpd/persistence/entities/FinalitePrincipale.java)
- [HistorisationGenerique.java](../../src/main/java/com/minds/rgpd/persistence/entities/HistorisationGenerique.java)
- [HistorisationRegistre.java](../../src/main/java/com/minds/rgpd/persistence/entities/HistorisationRegistre.java)
- [HistorisationTraitement.java](../../src/main/java/com/minds/rgpd/persistence/entities/HistorisationTraitement.java)
- [LiceiteTraitement.java](../../src/main/java/com/minds/rgpd/persistence/entities/LiceiteTraitement.java)
- [Preconisation.java](../../src/main/java/com/minds/rgpd/persistence/entities/Preconisation.java)
- [Profil.java](../../src/main/java/com/minds/rgpd/persistence/entities/Profil.java)
- [ResponsableTraitement.java](../../src/main/java/com/minds/rgpd/persistence/entities/ResponsableTraitement.java)
- [Sensibilite.java](../../src/main/java/com/minds/rgpd/persistence/entities/Sensibilite.java)
- [Traitement.java](../../src/main/java/com/minds/rgpd/persistence/entities/Traitement.java)
- [Violation.java](../../src/main/java/com/minds/rgpd/persistence/entities/Violation.java)

## `src/main/java/com/minds/rgpd/persistence/repositories`

- [ClientLogoRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/ClientLogoRepository.java)
- [ClientRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/ClientRepository.java)
- [DefinitionRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/DefinitionRepository.java)
- [DemandeRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/DemandeRepository.java)
- [DureeRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/DureeRepository.java)
- [EtablissementRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/EtablissementRepository.java)
- [HistorisationRegistreRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/HistorisationRegistreRepository.java)
- [HistorisationTraitementRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/HistorisationTraitementRepository.java)
- [PreconisationRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/PreconisationRepository.java)
- [ProfilRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/ProfilRepository.java)
- [ResponsableTraitementRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/ResponsableTraitementRepository.java)
- [TraitementRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/TraitementRepository.java)
- [ViolationRepository.java](../../src/main/java/com/minds/rgpd/persistence/repositories/ViolationRepository.java)

## `src/main/java/com/minds/rgpd/persistence/specifications`

- [EtablissementSpecifications.java](../../src/main/java/com/minds/rgpd/persistence/specifications/EtablissementSpecifications.java)
- [PreconisationSpecifications.java](../../src/main/java/com/minds/rgpd/persistence/specifications/PreconisationSpecifications.java)
- [TraitementSpecifications.java](../../src/main/java/com/minds/rgpd/persistence/specifications/TraitementSpecifications.java)
- [ViolationSpecifications.java](../../src/main/java/com/minds/rgpd/persistence/specifications/ViolationSpecifications.java)

## `src/main/java/com/minds/rgpd/web/controllers`

- [ClientController.java](../../src/main/java/com/minds/rgpd/web/controllers/ClientController.java)
- [DemandeController.java](../../src/main/java/com/minds/rgpd/web/controllers/DemandeController.java)
- [EtablissementController.java](../../src/main/java/com/minds/rgpd/web/controllers/EtablissementController.java)
- [FichierController.java](../../src/main/java/com/minds/rgpd/web/controllers/FichierController.java)
- [HistorisationController.java](../../src/main/java/com/minds/rgpd/web/controllers/HistorisationController.java)
- [PreconisationController.java](../../src/main/java/com/minds/rgpd/web/controllers/PreconisationController.java)
- [ProfilController.java](../../src/main/java/com/minds/rgpd/web/controllers/ProfilController.java)
- [TraitementController.java](../../src/main/java/com/minds/rgpd/web/controllers/TraitementController.java)
- [UtilisateurController.java](../../src/main/java/com/minds/rgpd/web/controllers/UtilisateurController.java)
- [ViolationController.java](../../src/main/java/com/minds/rgpd/web/controllers/ViolationController.java)

## `src/main/resources`

- [application-dev.yaml](../../src/main/resources/application-dev.yaml)
- [application-win.yaml](../../src/main/resources/application-win.yaml)
- [application.yaml](../../src/main/resources/application.yaml)
- [ehcache.xml](../../src/main/resources/ehcache.xml)

## `src/main/resources/db/migration`

- [V1.0__Script_de_creation.sql](../../src/main/resources/db/migration/V1.0__Script_de_creation.sql)
- [V11.0__Creation_table_violation.sql](../../src/main/resources/db/migration/V11.0__Creation_table_violation.sql)
- [V12.0__Registre_colonnes_complementaires_et_historisation.sql](../../src/main/resources/db/migration/V12.0__Registre_colonnes_complementaires_et_historisation.sql)
- [V13.0__Creation_table_client_logo.sql](../../src/main/resources/db/migration/V13.0__Creation_table_client_logo.sql)
- [V14.0__Ajout_colonnes_etablissement.sql](../../src/main/resources/db/migration/V14.0__Ajout_colonnes_etablissement.sql)
- [V2.0__Script_de_migration.sql](../../src/main/resources/db/migration/V2.0__Script_de_migration.sql)
- [V3.0__Modifications_type_colonnes_traitement.sql](../../src/main/resources/db/migration/V3.0__Modifications_type_colonnes_traitement.sql)
- [V4.0__Modifications_table_traitement.sql](../../src/main/resources/db/migration/V4.0__Modifications_table_traitement.sql)
- [V5.0__Suppression_colonne_gestionnaire.sql](../../src/main/resources/db/migration/V5.0__Suppression_colonne_gestionnaire.sql)
- [V6.0__Ajout_colonne_donnees_concernees.sql](../../src/main/resources/db/migration/V6.0__Ajout_colonne_donnees_concernees.sql)
- [V7.0__Evolution_Modele_de_donnees.sql](../../src/main/resources/db/migration/V7.0__Evolution_Modele_de_donnees.sql)
- [V8.0__Create_demande.sql](../../src/main/resources/db/migration/V8.0__Create_demande.sql)
- [V9.0__Creation_table_preconisation.sql](../../src/main/resources/db/migration/V9.0__Creation_table_preconisation.sql)

## `src/test/java/com/minds/rgpd`

- [AbstractITSpring.java](../../src/test/java/com/minds/rgpd/AbstractITSpring.java)

## `src/test/java/com/minds/rgpd/annotation`

- [DataJpaTestWithTestContainers.java](../../src/test/java/com/minds/rgpd/annotation/DataJpaTestWithTestContainers.java)

## `src/test/java/com/minds/rgpd/assertions`

- [OpenAPIInfoAssertor.java](../../src/test/java/com/minds/rgpd/assertions/OpenAPIInfoAssertor.java)

## `src/test/java/com/minds/rgpd/business/services/impl`

- [ClientServiceImplTest.java](../../src/test/java/com/minds/rgpd/business/services/impl/ClientServiceImplTest.java)
- [EtablissementServiceImplTest.java](../../src/test/java/com/minds/rgpd/business/services/impl/EtablissementServiceImplTest.java)
- [FichierExportTemplateTest.java](../../src/test/java/com/minds/rgpd/business/services/impl/FichierExportTemplateTest.java)
- [PreconisationServiceImplTest.java](../../src/test/java/com/minds/rgpd/business/services/impl/PreconisationServiceImplTest.java)
- [ProfilServiceImplTest.java](../../src/test/java/com/minds/rgpd/business/services/impl/ProfilServiceImplTest.java)
- [TraitementServiceImplTest.java](../../src/test/java/com/minds/rgpd/business/services/impl/TraitementServiceImplTest.java)
- [UtilisateurServiceImplTest.java](../../src/test/java/com/minds/rgpd/business/services/impl/UtilisateurServiceImplTest.java)

## `src/test/java/com/minds/rgpd/business/services/utilities`

- [Utilitaire.java](../../src/test/java/com/minds/rgpd/business/services/utilities/Utilitaire.java)

## `src/test/java/com/minds/rgpd/business/utilities`

- [AccessUserInformationTest.java](../../src/test/java/com/minds/rgpd/business/utilities/AccessUserInformationTest.java)
- [DefinitionResolverTest.java](../../src/test/java/com/minds/rgpd/business/utilities/DefinitionResolverTest.java)
- [DureeResolverTest.java](../../src/test/java/com/minds/rgpd/business/utilities/DureeResolverTest.java)
- [FormatImageTest.java](../../src/test/java/com/minds/rgpd/business/utilities/FormatImageTest.java)
- [ResponsableTraitementResolverTest.java](../../src/test/java/com/minds/rgpd/business/utilities/ResponsableTraitementResolverTest.java)
- [TraitementDiffTest.java](../../src/test/java/com/minds/rgpd/business/utilities/TraitementDiffTest.java)

## `src/test/java/com/minds/rgpd/business/utilities/mappers`

- [MappingCyclesTest.java](../../src/test/java/com/minds/rgpd/business/utilities/mappers/MappingCyclesTest.java)
- [TraitementMapperUpdateTest.java](../../src/test/java/com/minds/rgpd/business/utilities/mappers/TraitementMapperUpdateTest.java)

## `src/test/java/com/minds/rgpd/infrastructure/keycloak`

- [KeycloakAdminClientTest.java](../../src/test/java/com/minds/rgpd/infrastructure/keycloak/KeycloakAdminClientTest.java)
- [KeycloakIdentityGatewayTest.java](../../src/test/java/com/minds/rgpd/infrastructure/keycloak/KeycloakIdentityGatewayTest.java)
- [NoopIdentityGateway.java](../../src/test/java/com/minds/rgpd/infrastructure/keycloak/NoopIdentityGateway.java)

## `src/test/java/com/minds/rgpd/infrastructure/security`

- [TestSecurityConfig.java](../../src/test/java/com/minds/rgpd/infrastructure/security/TestSecurityConfig.java)

## `src/test/java/com/minds/rgpd/integrationtest`

- [ClientLogoIT.java](../../src/test/java/com/minds/rgpd/integrationtest/ClientLogoIT.java)
- [ControllersIT.java](../../src/test/java/com/minds/rgpd/integrationtest/ControllersIT.java)
- [EnvoiFichierIT.java](../../src/test/java/com/minds/rgpd/integrationtest/EnvoiFichierIT.java)
- [TraitementControllerIT.java](../../src/test/java/com/minds/rgpd/integrationtest/TraitementControllerIT.java)
- [TraitementReferentielIT.java](../../src/test/java/com/minds/rgpd/integrationtest/TraitementReferentielIT.java)

## `src/test/java/com/minds/rgpd/persistence/repositories`

- [EtablissementRepositoryTest.java](../../src/test/java/com/minds/rgpd/persistence/repositories/EtablissementRepositoryTest.java)
- [TraitementRepositoryTest.java](../../src/test/java/com/minds/rgpd/persistence/repositories/TraitementRepositoryTest.java)

## `src/test/java/com/minds/rgpd/persistence/specifications`

- [TraitementSpecificationsTest.java](../../src/test/java/com/minds/rgpd/persistence/specifications/TraitementSpecificationsTest.java)

## `src/test/java/com/minds/rgpd/testcontainers`

- [TestContainersConfiguration.java](../../src/test/java/com/minds/rgpd/testcontainers/TestContainersConfiguration.java)

## `src/test/resources`

- [application-test.yaml](../../src/test/resources/application-test.yaml)
- [docker-compose-test.yml](../../src/test/resources/docker-compose-test.yml)

## `src/test/resources/rgpdFile`

- [La breteche_CREATIVE_Registre RGPD_ed3.25.xlsx](../../src/test/resources/rgpdFile/La%20breteche_CREATIVE_Registre%20RGPD_ed3.25.xlsx)

## `src/test/resources/scripts`

- [initialisation_import_fichier_TI.sql](../../src/test/resources/scripts/initialisation_import_fichier_TI.sql)

## Racine et outillage

- [pom.xml](../../pom.xml)
- [Jenkinsfile](../../Jenkinsfile)
- [docker-compose.yml](../../docker-compose.yml)
- [sonar-project.properties](../../sonar-project.properties)
- [project-suppression-cve.xml](../../project-suppression-cve.xml)
- [.gitignore](../../.gitignore)