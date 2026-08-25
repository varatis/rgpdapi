package com.minds.rgpd.business.Imports;

import com.minds.rgpd.business.utilities.DefinitionResolver;
import com.minds.rgpd.business.utilities.DureeResolver;
import com.minds.rgpd.business.utilities.ResponsableTraitementResolver;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Etablissement;
import com.minds.rgpd.persistence.entities.Preconisation;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.EtablissementRepository;
import com.minds.rgpd.persistence.repositories.PreconisationRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class ImportSpecifications {
    private final TraitementRepository traitementRepository;
    private final EtablissementRepository etablissementRepository;
    private final PreconisationRepository preconisationRepository;
    private final DefinitionResolver definitionResolver;
    private final DureeResolver dureeResolver;
    private final ResponsableTraitementResolver responsableTraitementResolver;

    public ImportSpecification<Traitement> traitement(Client client, String version)
    {
        // Cache local à l'import : les établissements ne sont plus enregistrés au fil des
        // lignes, donc une recherche en base ne verrait pas celui créé quelques lignes plus
        // haut. Sans ce cache, deux lignes citant le même nouvel établissement en créeraient
        // deux exemplaires.
        // Les référentiels (définitions, durées, responsables) n'ont pas besoin de ce cache :
        // leurs résolveurs enregistrent immédiatement, donc la ligne suivante les retrouve.
        Map<String, Etablissement> etablissementsDuFichier = new HashMap<>();

        return new ImportSpecification<Traitement>(
            "Registre de traitement",
            false,
            6,
            List.of(
                "ID",
                "Nom du traitement",
                "Date d'identification du traitement"
            ),
            row -> {
                Traitement.TraitementBuilder traitementBuilder = Traitement.builder();
                traitementBuilder.idFonctionnel(row.getInt("ID"));
                traitementBuilder.donneesConcernees(row.getString("Données concernées"));
                traitementBuilder.nom(row.getString("Nom du traitement"));
                traitementBuilder.dateIdentification(row.getDate("Date d'identification du traitement"));
                traitementBuilder.dateMiseAJour(row.getDate("Date de mise à jour"));
                traitementBuilder.historiqueModifications(row.getString("Historique des modifications"));
                traitementBuilder.dataProtectionOfficer(row.getString("Data Protection Officer"));
                traitementBuilder.responsableTraitement(responsableTraitementResolver
                        .resolveResponsableTraitement(row.getString("Responsable de traitement"), client));
                traitementBuilder.gestionnaireMiseEnOeuvre(row.getString("Gestionnaire de la mise en œuvre du traitement"));
                traitementBuilder.finalitePrincipale(definitionResolver
                        .resolveFinalitePrincipale(row.getString("Finalité principale"), client));
                traitementBuilder.sousFinalites(row.getString("Sous-finalités"));
                traitementBuilder.categoriesPersonnesConcernees(row.getString("Catégories de personnes concernées par le traitement"));
                traitementBuilder.donneesIdentification(row.getString("Données d'identification"));
                traitementBuilder.donneesConnexion(row.getString("Données de connexion"));
                traitementBuilder.donneesLocalisation(row.getString("Données de localisation"));
                traitementBuilder.donneesComportementViePerso(row.getString("Données sur le comportement et la vie personnelle"));
                traitementBuilder.donneesEconomiquesFinancieres(row.getString("Données économiques et financières"));
                traitementBuilder.donneesProfessionnelles(row.getString("Données professionnelles"));
                traitementBuilder.categoriesParticulieresDonnees(row.getString("Catégories particulières de données (NIR, santé par exemple)"));
                traitementBuilder.sensibilite(definitionResolver
                        .resolveSensibilite(row.getString("Sensibilité"), client));
                traitementBuilder.etudeImpact(definitionResolver
                        .resolveEtudeImpact(row.getString("Etude d'impact (PIA)"), client));
                traitementBuilder.canauxCollecteDonnees(row.getString("Canaux de collecte des données"));
                traitementBuilder.licieteTraitement(definitionResolver
                        .resolveLiceiteTraitement(row.getString("Licéité du traitement"), client));
                traitementBuilder.recoursTraitementAutomatises(row.getBoolean("Recours au traitements automatisés (y compris profilage) ? (Oui / Non)"));
                traitementBuilder.emplacementPhysique(row.getString("Emplacement physique du traitement"));
                traitementBuilder.dispositionsSecuriteDonneesPhysique(row.getString("Dispositions existantes pour assurer la sécurité des données", 0));
                traitementBuilder.emplacementNumerique(row.getString("Emplacement numérique du traitement"));
                traitementBuilder.dispositionsSecuriteDonneesNumerique(row.getString("Dispositions existantes pour assurer la sécurité des données",1));
                traitementBuilder.hebergement(row.getString("Hébergement"));
                traitementBuilder.dureeConservation(dureeResolver
                        .resolveDureeConservation(row.getString("Durée de conservation"), client));
                traitementBuilder.archivage(row.getBoolean("Archivage ? (Oui / Non)"));
                traitementBuilder.dureeArchivage(dureeResolver
                        .resolveDureeArchivage(row.getString("Durée d'archivage"), client));
                traitementBuilder.categoriesDestinataires(row.getString("Catégories de destinataires"));
                traitementBuilder.raisonsTransfertDestinataires(row.getString("Raisons du transfert vers les catégories de destinataires"));
                traitementBuilder.transfertsHorsUE(row.getBoolean("Transferts hors UE (Oui / Non)"));
                traitementBuilder.paysDestinataires(row.getString("Pays destinataires"));
                traitementBuilder.commentaires(row.getString("Commentaires"));

                String rawEtablissementNames = row.getString("Etablissement(s)");
                var etablissements = findOrCreateEtablissements(
                        rawEtablissementNames, client, etablissementsDuFichier);
                traitementBuilder.etablissements(etablissements);
                traitementBuilder.client(client);
                traitementBuilder.version(parseVersion(version));


                return traitementBuilder.build();
            },
            traitement -> !traitementRepository.findByAllBusinessFields(
                traitement.getNom(),
                traitement.getClient(),
                traitement.getGestionnaireMiseEnOeuvre(),
                Objects.isNull(traitement.getFinalitePrincipale()) ? null : traitement.getFinalitePrincipale().getValeur(),
                traitement.getDateIdentification()
                ).isEmpty(),
            traitementRepository
        );
    }

    private List<Etablissement> findOrCreateEtablissements(String etablissementsRaw, Client client, Map<String, Etablissement> cache)
    {
        if (etablissementsRaw == null || etablissementsRaw.isBlank()) {
            return List.of();
        }
        return etablissementsRaw.lines()
            .filter(line -> !line.isBlank())
            .map(String::trim)  // Nettoyage des espaces
            .distinct()         // Évite les doublons dans le même fichier
            .map(nom -> cache.computeIfAbsent(nom, n -> findOrCreateEtablissement(n, client)))
            .toList();
    }
    /**
     * Extraction de la logique pour un seul établissement
     */
    private Etablissement findOrCreateEtablissement(String nom, Client client) {
        return etablissementRepository.findByNom(nom)
            .orElseGet(() -> createEtablissement(nom, client));
    }
    /**
     * Séparation création/sauvegarde
     */
    private Etablissement createEtablissement(String nom, Client client) {
        Etablissement newEtablissement = Etablissement.builder()
            .id(UUID.randomUUID())
            .nom(nom)
            .client(client)
            .build();
        return newEtablissement;
    }
    private int parseVersion(String version) {
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public ImportSpecification<Preconisation> preconisation(Client client, String sheetName) {
        return new ImportSpecification<>(
                sheetName,
                true,
                1,
                List.of("Préconisation"),
                row -> {
                    Preconisation.PreconisationBuilder builder = Preconisation.builder();
                    builder.libelle(row.getString("Préconisation"));
                    builder.explication(row.getOptionalString("Explication"));
                    builder.risqueEncours(row.getOptionalString("Risque encouru"));
                    builder.contraintes(row.getOptionalString("Contraintes"));
                    builder.cout(row.getOptionalString("Cout", "Coût"));
                    builder.priorite(row.getOptionalString("Priorité"));
                    builder.complexite(row.getOptionalString("Complexité"));
                    builder.commentaire(row.getOptionalString("Commentaire"));
                    builder.etatAvancement(row.getOptionalString(
                            "État d'avancement",
                            "Etat d'avancement",
                            "Avancement",
                            "Statut"
                    ));
                    builder.client(client);
                    builder.traitement(resolveTraitement(row, client));
                    return builder.build();
                },
                preconisation -> !preconisationRepository.findDuplicates(
                        preconisation.getClient(),
                        preconisation.getLibelle(),
                        preconisation.getTraitement()
                ).isEmpty(),
                preconisationRepository
        );
    }

    private Traitement resolveTraitement(ExcelRow row, Client client) {
        Integer idFonctionnel = readOptionalInt(row, "ID", "ID traitement", "Id traitement");
        if (idFonctionnel != null) {
            return traitementRepository.findByIdFonctionnelAndClient(idFonctionnel, client)
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
        String nomTraitement = row.getOptionalString("Nom du traitement");
        if (nomTraitement == null) {
            return null;
        }
        return traitementRepository.findByNomAndClient(nomTraitement, client)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Integer readOptionalInt(ExcelRow row, String... columnNames) {
        for (String columnName : columnNames) {
            if (!row.hasColumn(columnName) || row.isEmpty(columnName)) {
                continue;
            }
            try {
                return row.getInt(columnName);
            } catch (ExcelParsingException e) {
                return null;
            }
        }
        return null;
    }
}