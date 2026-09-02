# Registre de traitements — contrat d'API

Ce document récapitule les points d'entrée du module *Registre de traitements* et les
règles de gestion qu'ils implémentent.

| Règle | Objet | Où c'est implémenté |
|-------|-------|---------------------|
| RG1 | Toute modification d'un traitement est historisée | `HistorisationService`, `TraitementServiceImpl`, `TraitementDiff` |
| RG2 | L'import remplace l'état précédent des traitements du client | `FichierServiceImpl.remplacerRegistre` |
| RG3 | L'utilisateur est informé des conséquences avant l'import | `GET /importFichierRgpd/apercu`, réponse `409` de `POST /importFichierRgpd` |
| RG4 | La version du registre est reprise du fichier importé | `FichierServiceImpl.mettreAJourVersionRegistre` |
| RG5 | Les colonnes complémentaires du registre sont importées | `ImportSpecifications.mapColonnesComplementaires` |

## Import (RG2 / RG3)

### 1. Aperçu — avant même d'envoyer le fichier

```
GET /importFichierRgpd/apercu?nomFichier=La%20breteche_CREATIVE_Registre%20RGPD_ed3.25.xlsx
```

```json
{
  "nomFichier": "La breteche_CREATIVE_Registre RGPD_ed3.25.xlsx",
  "clientNom": "La breteche",
  "fichierValide": true,
  "messageErreur": null,
  "versionActuelle": "3.24",
  "dateVersionActuelle": "2026-01-12",
  "versionFichier": "3.25",
  "remplacementDonnees": true,
  "nombreTraitementsExistants": 80,
  "nombrePreconisationsExistantes": 12,
  "nombreViolationsExistantes": 0,
  "avertissement": "L'import va remplacer la totalité du registre de La breteche : …",
  "urlExportPrealable": "/importFichierRgpd/export"
}
```

C'est la source de la modale : `avertissement` est le texte à afficher,
`urlExportPrealable` le lien « Exporter le registre actuel ».

### 2. Import

```
POST /importFichierRgpd            (multipart : file, confirmerRemplacement)
```

- `confirmerRemplacement=false` (défaut) **et** registre non vide →
  **HTTP 409** avec `confirmationRequise: true` et le champ `apercu` renseigné.
  Rien n'est modifié en base.
- `confirmerRemplacement=true` → l'import s'exécute : les traitements,
  préconisations et violations du client sont supprimés puis remplacés par le
  contenu du fichier, dans une seule transaction.

Réponse en cas de succès :

```json
{
  "nomFichier": "…", "dateReception": "…", "dateFinTraitement": "…",
  "statusFichier": "OK",
  "confirmationRequise": false,
  "version": "3.25",
  "nombreTraitementsRemplaces": 80,
  "nombreTraitementsImportes": 80
}
```

Ce que l'import **ne** supprime **pas** : le référentiel du client (définitions,
durées, responsables de traitement) et ses établissements, réutilisés par les
lignes importées.

### 3. Export (sauvegarde préalable)

```
GET /importFichierRgpd/export
```

Le fichier produit porte les mêmes en-têtes, au même emplacement (ligne 6,
à partir de la colonne B) et le même nom (`<client>_CREATIVE_Registre RGPD_ed<version>.xlsx`)
que ceux attendus par l'import : il est donc réimportable tel quel pour revenir
en arrière.

## Historique (RG1 / CA4)

```
GET  /traitements/{idFonctionnel}/historique      → HistorisationDTO[]
POST /traitements/{idFonctionnel}/historique      { "motif": "…", "date": "…" (optionnel) }
GET  /registre/historique?clientNom=…             → HistorisationDTO[]
POST /registre/historique?clientNom=…             { "motif": "…" }
```

```json
{ "id": 42, "date": "2026-09-02T11:20:00", "motif": "nom : « Paie » → « Paie et primes »", "auteur": "Alice Dupont" }
```

- Les entrées **automatiques** sont créées par le back à la création, la
  modification et la suppression d'un traitement, ainsi qu'à chaque import. Le
  motif liste les champs modifiés avec leurs valeurs avant/après.
- Les entrées **manuelles** (CA4) passent par les `POST` ci-dessus. `auteur` est
  toujours déduit du jeton, jamais du corps de la requête.
- `TraitementDTO` expose également `historiqueTraitement` (lecture seule) pour
  afficher l'historique dans le détail d'un traitement sans second appel.

## Colonnes complémentaires (RG5)

Ajoutées à `TraitementDTO` et à la table `traitement` :

- **Analyse de conformité** : `impactTraitement`, `detournementFinalite` +
  `scoreDetournementFinalite`, `collecteDcpInappropriees` + `score…`,
  `conservationExcessiveDcp`, `securisationInsuffisanteDcp`, `vicesConsentement`,
  `manqueTransparence`, `incapaciteExerciceDroits`, `transfertTiersMalEncadre`,
  `transfertHorsUeAbusif`, `defautPreuve`, `scoreGlobal`, `commentairesAnalyse`,
  `expositionTraitement` (entiers, sauf le commentaire).
- **Critères PIA** (booléens, cochés d'une croix dans le fichier) :
  `critereEvaluationScoring`, `critereDecisionAutomatique`,
  `critereSurveillanceSystematique`, `critereCollecteDonneesSensibles`,
  `critereCollecteLargeEchelle`, `critereCroisementDonnees`,
  `criterePersonnesVulnerables`, `critereUsageInnovant`,
  `critereExclusionBeneficeDroit`.

Ces colonnes sont facultatives dans le fichier : leur absence n'empêche pas
l'import.

## Version du registre (RG4)

La version est extraite du nom de fichier (`…_ed3.25.xlsx` → `3.25`) et écrite
sur le client (`client.version`, `client.dateVersion`) à l'issue d'un import
réussi. Le changement de version est tracé dans l'historique du registre.
