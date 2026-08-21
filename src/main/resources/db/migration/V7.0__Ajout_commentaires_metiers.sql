-- ============================================================================
-- V7.0 - Documentation métier du modèle de données (COMMENT ON ...)
-- ----------------------------------------------------------------------------
-- Reporte les définitions métier du registre des activités de traitement sur
-- les tables et colonnes correspondantes, afin de les rendre visibles dans
-- pgAdmin / DBeaver et toute inspection du schéma (information_schema).
--
-- Provenance des définitions (voir docs/mapping-bdd-registre.md) :
--   [REGISTRE] = définition reprise de l'onglet « FR_Définitions » du fichier
--                registre (ed3.25), éventuellement épurée des mentions propres
--                à Excel ou complétée du libellé exact de la colonne source.
--   [PROPOSÉE] = définition rédigée à partir de l'usage constaté dans le
--                registre et du contexte RGPD, A VALIDER par le métier
--                (tracé dans docs/mapping-bdd-registre.md).
--
-- Aucune structure n'est modifiée : COMMENT ON remplace uniquement le
-- commentaire existant sur l'objet visé.
-- ============================================================================


-- ============================================================================
-- TABLE traitement — Registre des activités de traitement
-- ============================================================================

COMMENT ON TABLE traitement IS $def$
Registre des activités de traitement (Art. 30 RGPD) : une ligne décrit un
traitement de données à caractère personnel d'un client (organisme).
[PROPOSÉE]
$def$;

-- --- Identification du traitement -------------------------------------------

COMMENT ON COLUMN traitement.identifiant IS $def$
Identifiant technique unique du traitement (UUID généré par l'application).
À ne pas confondre avec id_fonctionnel, la clé métier issue du registre.
[PROPOSÉE]
$def$;

COMMENT ON COLUMN traitement.id_fonctionnel IS $def$
Numéro unique d'identification du traitement (colonne « ID » du registre).
Clé métier issue du fichier registre source, distincte de l'identifiant
technique UUID.
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.nom IS $def$
Le nom du traitement doit être suffisamment explicite pour que l'on comprenne
de manière macro ce que fait le traitement.
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.donnees_concernees IS $def$
Personnes concernées par le traitement telles que saisies dans la colonne
« Données concernées » du registre (liste libre, ex. « Demandeurs »,
« Familles et proches des demandeurs »).
Voir aussi categories_personnes_concernees pour la liste normalisée.
[PROPOSÉE]
$def$;

COMMENT ON COLUMN traitement.date_identification IS $def$
Date à laquelle le traitement a été créé dans le registre.
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.date_mise_a_jour IS $def$
Date à laquelle les informations sur le traitement ont été mises à jour pour
la dernière fois.
[REGISTRE — définition épurée de la mention du raccourci Excel]
$def$;

COMMENT ON COLUMN traitement.historique_modifications IS $def$
Liste de l'ensemble des modifications qui ont été réalisées dans le registre
sur le traitement, afin d'assurer une traçabilité des actions.
Colonne « Historique des modifications » du registre.
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.version IS $def$
Édition du fichier registre source à l'origine du traitement, extraite du nom
de fichier (« <client>_<etablissement>_Registre RGPD_ed<édition>.xlsx ») lors
de l'import ; nulle pour un traitement créé hors import.
[PROPOSÉE]
$def$;

COMMENT ON COLUMN traitement.data_protection_officer IS $def$
Nom et coordonnées de la personne ayant le rôle de DPO pour le traitement.
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.responsable_traitement IS $def$
Fonction de la personne ayant le rôle de responsable de traitement, c'est-à-dire
qui détermine les finalités et les moyens du traitement ; souvent un membre de
la Direction Générale de l'entreprise.
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.gestionnaire_mise_en_oeuvre IS $def$
Fonction du représentant de l'entité ou du service qui est en charge de mettre
en œuvre le traitement (ex. : pour le traitement des CV des candidats, le
responsable de l'équipe recrutement).
Colonne « Gestionnaire de la mise en œuvre du traitement » du registre, libellée
« Représentant de l'entité responsable de la mise en œuvre du traitement » dans
l'onglet FR_Définitions.
[REGISTRE — écart de libellé signalé dans docs/mapping-bdd-registre.md]
$def$;

COMMENT ON COLUMN traitement.id_client IS $def$
Client (organisme) propriétaire du traitement ; assure le cloisonnement
multi-clients des données du registre.
[PROPOSÉE]
$def$;

-- --- Finalités --------------------------------------------------------------

COMMENT ON COLUMN traitement.finalite_principale IS $def$
Objectif final spécifique, explicite et légitime pour lequel le traitement a lieu.
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.sous_finalites IS $def$
Les sous-finalités doivent être rattachées à la finalité principale du traitement.
[REGISTRE]
$def$;

-- --- Données personnelles traitées ------------------------------------------

COMMENT ON COLUMN traitement.categories_personnes_concernees IS $def$
Catégories de personnes concernées par le traitement.
Exemples : employés, prestataires, clients, partenaires…
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.donnees_identification IS $def$
Données d'identification traitées.
Exemples : nom, prénom, adresse postale, numéro de téléphone, adresse e-mail,
photos, vidéos…
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.donnees_connexion IS $def$
Données de connexion traitées.
Exemples : adresse IP, logs, cookies, historique de navigation…
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.donnees_localisation IS $def$
Données de localisation traitées.
Exemples : positionnement GPS, GSM…
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.donnees_comportement_vie_perso IS $def$
Données sur le comportement et la vie personnelle.
Exemples : situation familiale, habitudes de vie, habitudes de consommation…
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.donnees_economiques_financieres IS $def$
Données économiques et financières.
Exemples : revenus, situation financière, situation fiscale, numéro de carte
bancaire, RIB…
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.donnees_professionnelles IS $def$
Données professionnelles.
Exemples : nom de l'employeur, statut dans l'entreprise, contrat de travail…
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.categories_particulieres_donnees IS $def$
Catégories particulières de données (Art. 9 et 10 RGPD).
Exemples : origine raciale ou ethnique, opinions politiques, convictions
religieuses ou philosophiques ou appartenance syndicale, données génétiques,
données biométriques, données concernant la santé, la vie sexuelle ou
l'orientation sexuelle, données relatives aux condamnations pénales et aux
infractions, NIR…
La liste de référence des données sensibles figure dans l'onglet FR_Définitions
du registre source.
[REGISTRE — complétée du « NIR » présent dans le libellé de la colonne registre]
$def$;

COMMENT ON COLUMN traitement.sensibilite IS $def$
Type(s) de données sensibles effectivement traitées, choisis dans la liste de
référence de l'onglet FR_Définitions du registre (ex. « NIR (N° SS) »,
« Concernant la santé », « Pas de donnée sensible »…).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN traitement.etude_impact IS $def$
Cas rendant une étude d'impact (PIA/AIPD) obligatoire pour le traitement
(ex. données sensibles Art. 9, traitement à grande échelle…), choisi parmi les
critères de référence listés dans l'onglet FR_Définitions du registre
(Art. 35 RGPD).
[PROPOSÉE]
$def$;

-- --- Description du traitement ----------------------------------------------

COMMENT ON COLUMN traitement.canaux_collecte_donnees IS $def$
Moyens par lesquels les données sont récupérées / collectées pour le traitement.
Par exemple : mail, formulaire papier, caméras…
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.liceite_traitement IS $def$
Base légale du traitement (Art. 6 RGPD) : le traitement n'est licite que si la
personne concernée a donné son consentement au traitement de ses données, si le
traitement est nécessaire à l'exécution d'un contrat auquel la personne concernée
est partie ou si le traitement est nécessaire au respect d'une obligation légale.
Valeurs choisies dans la liste de référence « Licéité du traitement (Article 6) »
de l'onglet FR_Définitions du registre.
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.recours_traitements_automatises IS $def$
Recours à un traitement non basé sur une intervention ou analyse humaine,
y compris toute forme de traitement automatisé consistant à utiliser les données
à caractère personnel pour évaluer ou prédire des éléments la concernant
(santé, situation économique, préférences personnelles, comportement,
déplacements…).
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.emplacement_physique IS $def$
Lieu physique de conservation des données du traitement (ex. : bureau, armoire
fermée à clé, archives papier…).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN traitement.dispositions_securite_donnees_physique IS $def$
Dispositifs en place pour assurer la sécurité des données conservées
physiquement (ex. : classeur dans une armoire fermée à clé).
Il est possible de faire référence à des documents tels que le Dossier
d'Architecture Technique, le PIA (Privacy Impact Assessment) ou toute autre
documentation des mesures de sécurité « standard » existantes dans l'entreprise.
Colonne « Dispositions existantes pour assurer la sécurité des données » du
registre, dupliquée en deux colonnes (physique / numérique).
[REGISTRE — déclinée sur le périmètre physique]
$def$;

COMMENT ON COLUMN traitement.emplacement_numerique IS $def$
Localisation numérique des données du traitement (ex. : applications, serveurs
de fichiers, messagerie…).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN traitement.dispositions_securite_donnees_numerique IS $def$
Dispositifs en place pour assurer la sécurité des données conservées
numériquement (ex. : authentification par utilisateur + mot de passe).
Il est possible de faire référence à des documents tels que le Dossier
d'Architecture Technique, le PIA (Privacy Impact Assessment) ou toute autre
documentation des mesures de sécurité « standard » existantes dans l'entreprise.
Colonne « Dispositions existantes pour assurer la sécurité des données » du
registre, dupliquée en deux colonnes (physique / numérique).
[REGISTRE — déclinée sur le périmètre numérique]
$def$;

COMMENT ON COLUMN traitement.hebergement IS $def$
Hébergeur(s) des données du traitement (ex. : éditeurs, fournisseurs cloud,
hébergement interne, État…).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN traitement.duree_conservation IS $def$
Durée de conservation pour les archives dites courantes : les archives courantes
sont réservées à l'utilisation courante des données par les services responsables
de la mise en œuvre du traitement (ex. : « 5 ans », « Supprimer »).
Colonne « Durée de conservation » du registre, correspondant à
« Durée d'archivage courant » dans l'onglet FR_Définitions.
[REGISTRE — écart de libellé signalé dans docs/mapping-bdd-registre.md]
$def$;

COMMENT ON COLUMN traitement.archivage IS $def$
Indique si les données font l'objet d'un archivage à l'issue de leur durée de
conservation (colonne « Archivage ? (Oui / Non) » du registre).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN traitement.duree_archivage IS $def$
Durée de conservation des archives dites définitives : les archives définitives
sont réservées aux archives présentant un intérêt historique, statistique ou
scientifique.
Colonne « Durée d'archivage » du registre, correspondant à
« Durée d'archivage définitif » dans l'onglet FR_Définitions.
[REGISTRE — écart de libellé signalé dans docs/mapping-bdd-registre.md]
$def$;

-- --- Destinataires et transferts --------------------------------------------

COMMENT ON COLUMN traitement.categories_destinataires IS $def$
Toute personne, service, entreprise filiale ou tiers ayant un accès aux données
à caractère personnel du traitement.
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.raisons_transfert_destinataires IS $def$
Intérêts des destinataires à recevoir les données à caractère personnel
transférées.
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.transferts_hors_ue IS $def$
Communications de données à caractère personnel qui font ou sont destinées à
faire l'objet d'un traitement en dehors des États membres de l'Union Européenne.
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.pays_destinataires IS $def$
Pays dans lesquels sont basés les catégories de destinataires à qui les données
sont communiquées.
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement.commentaires IS $def$
Informations complémentaires en texte libre sur le traitement.
[PROPOSÉE]
$def$;


-- ============================================================================
-- TABLE traitement_etablissement — Établissements concernés par un traitement
-- ============================================================================

COMMENT ON TABLE traitement_etablissement IS $def$
Établissement(s) concerné(s) par un traitement (liaison N-N) : si l'organisme a
plusieurs établissements, indique le ou les établissements concernés par ce
traitement (colonne « Etablissement(s) » du registre).
[REGISTRE]
$def$;

COMMENT ON COLUMN traitement_etablissement.id IS $def$
Identifiant technique de la liaison (UUID généré par la base).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN traitement_etablissement.id_traitement IS $def$
Traitement concerné (référence vers traitement.identifiant, suppression en cascade).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN traitement_etablissement.id_etablissement IS $def$
Établissement concerné par le traitement (référence vers etablissement.id,
suppression en cascade).
[PROPOSÉE]
$def$;


-- ============================================================================
-- TABLES SUPPORT (hors registre — définitions [PROPOSÉE])
-- ============================================================================

-- --- client ------------------------------------------------------------------

COMMENT ON TABLE client IS $def$
Organisme client de la plateforme (tenant) : toutes les données métier
(utilisateurs, établissements, traitements) sont rattachées à un client.
[PROPOSÉE]
$def$;

COMMENT ON COLUMN client.id IS $def$
Identifiant technique unique du client (UUID généré par l'application).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN client.nom IS $def$
Nom (raison sociale ou nom d'usage) de l'organisme client.
[PROPOSÉE]
$def$;

COMMENT ON COLUMN client.statut IS $def$
Statut du client (ex. : actif, inactif).
[PROPOSÉE]
$def$;

-- --- etablissement -----------------------------------------------------------

COMMENT ON TABLE etablissement IS $def$
Établissement (site, service) d'un client ; un traitement du registre peut
concerner un ou plusieurs établissements.
[PROPOSÉE]
$def$;

COMMENT ON COLUMN etablissement.id IS $def$
Identifiant technique de l'établissement (UUID généré lors de l'import ou de la
création).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN etablissement.nom IS $def$
Nom de l'établissement, tel qu'indiqué dans la colonne « Etablissement(s) » du
registre.
[PROPOSÉE]
$def$;

COMMENT ON COLUMN etablissement.id_client IS $def$
Client propriétaire de l'établissement (référence vers client.id, suppression en
cascade).
[PROPOSÉE]
$def$;

-- --- profil ------------------------------------------------------------------

COMMENT ON TABLE profil IS $def$
Profil fonctionnel d'un utilisateur, déterminant ses droits dans l'application
(ex. : ADMIN, USER, DPO).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN profil.id IS $def$
Identifiant technique du profil (UUID).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN profil.code IS $def$
Code unique du profil (ex. : ADMIN, USER, DPO).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN profil.description IS $def$
Description du profil et de son périmètre de droits.
[PROPOSÉE]
$def$;

-- --- utilisateur -------------------------------------------------------------

COMMENT ON TABLE utilisateur IS $def$
Utilisateur de l'application, rattaché à un client et à un profil.
[PROPOSÉE]
$def$;

COMMENT ON COLUMN utilisateur.id IS $def$
Identifiant technique de l'utilisateur (UUID).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN utilisateur.prenom IS $def$
Prénom de l'utilisateur.
[PROPOSÉE]
$def$;

COMMENT ON COLUMN utilisateur.nom IS $def$
Nom de l'utilisateur.
[PROPOSÉE]
$def$;

COMMENT ON COLUMN utilisateur.email IS $def$
Adresse e-mail de l'utilisateur ; unique, sert d'identifiant de connexion.
[PROPOSÉE]
$def$;

COMMENT ON COLUMN utilisateur.password IS $def$
Mot de passe de l'utilisateur (stocké hashé).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN utilisateur.fonction IS $def$
Fonction de l'utilisateur au sein de l'organisme client (ex. : Responsable IT,
DPO).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN utilisateur.id_profil IS $def$
Profil (droits) de l'utilisateur (référence vers profil.id, suppression
restreinte).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN utilisateur.id_client IS $def$
Client auquel l'utilisateur est rattaché (référence vers client.id, suppression
en cascade).
[PROPOSÉE]
$def$;

-- --- utilisateur_etablissement -----------------------------------------------

COMMENT ON TABLE utilisateur_etablissement IS $def$
Affectation des utilisateurs aux établissements de leur client (liaison N-N).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN utilisateur_etablissement.id IS $def$
Identifiant technique de la liaison (UUID généré par la base).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN utilisateur_etablissement.id_utilisateur IS $def$
Utilisateur affecté (référence vers utilisateur.id, suppression en cascade).
[PROPOSÉE]
$def$;

COMMENT ON COLUMN utilisateur_etablissement.id_etablissement IS $def$
Établissement d'affectation (référence vers etablissement.id, suppression en
cascade).
[PROPOSÉE]
$def$;
