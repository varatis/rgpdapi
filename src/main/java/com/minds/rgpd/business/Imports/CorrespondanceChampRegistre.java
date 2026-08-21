package com.minds.rgpd.business.Imports;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Correspondance technique entre les libellés de l'onglet « FR_Définitions » du fichier
 * registre et le modèle de données (table / colonne).
 * <p>
 * Seule la CORRESPONDANCE est codée ici : les définitions métier (valeurs) restent dans
 * le fichier Excel et sont importées en base (table {@code definition_champ}).
 * <ul>
 *   <li>Un libellé peut cibler plusieurs colonnes (ex. : « Dispositions existantes… »,
 *       dupliqué en colonnes physique et numérique dans le registre).</li>
 *   <li>Un libellé peut ne cibler aucune colonne (ex. : « Responsable(s) conjoint(s) du
 *       traitement ») : la définition est tout de même importée, avec des cibles nulles,
 *       ce qui rend l'écart visible en base (cf. incohérences I1/I2 de
 *       {@code docs/mapping-bdd-registre.md}).</li>
 *   <li>« Etablissement(s) » porte sur la relation N-N {@code traitement_etablissement} :
 *       table ciblée, colonne nulle.</li>
 * </ul>
 */
public enum CorrespondanceChampRegistre {

    ID("ID", new Cible("traitement", "id_fonctionnel")),
    ETABLISSEMENTS("Etablissement(s)", new Cible("traitement_etablissement", null)),
    NOM("Nom du traitement", new Cible("traitement", "nom")),
    DATE_IDENTIFICATION("Date d'identification du traitement", new Cible("traitement", "date_identification")),
    DATE_MISE_A_JOUR("Date de mise à jour", new Cible("traitement", "date_mise_a_jour")),
    HISTORIQUE_MODIFICATIONS("Historique de modifications", new Cible("traitement", "historique_modifications")),
    DATA_PROTECTION_OFFICER("Data Protection Officer", new Cible("traitement", "data_protection_officer")),
    RESPONSABLE_TRAITEMENT("Responsable de traitement", new Cible("traitement", "responsable_traitement")),
    REPRESENTANT_MISE_EN_OEUVRE("Représentant de l'entité responsable de la mise en œuvre du traitement",
            new Cible("traitement", "gestionnaire_mise_en_oeuvre")),
    FINALITE_PRINCIPALE("Finalité principale", new Cible("traitement", "finalite_principale")),
    SOUS_FINALITES("Sous-finalités", new Cible("traitement", "sous_finalites")),
    CATEGORIES_PERSONNES_CONCERNEES("Catégories de personnes concernées par le traitement",
            new Cible("traitement", "categories_personnes_concernees")),
    DONNEES_IDENTIFICATION("Données d'identification", new Cible("traitement", "donnees_identification")),
    DONNEES_CONNEXION("Données de connexion", new Cible("traitement", "donnees_connexion")),
    DONNEES_LOCALISATION("Données de localisation", new Cible("traitement", "donnees_localisation")),
    DONNEES_COMPORTEMENT_VIE_PERSO("Données sur le comportement et la vie personnelle",
            new Cible("traitement", "donnees_comportement_vie_perso")),
    DONNEES_ECONOMIQUES_FINANCIERES("Données économiques et financières",
            new Cible("traitement", "donnees_economiques_financieres")),
    DONNEES_PROFESSIONNELLES("Données professionnelles", new Cible("traitement", "donnees_professionnelles")),
    CATEGORIES_PARTICULIERES_DONNEES("Catégories particulières de données",
            new Cible("traitement", "categories_particulieres_donnees")),
    CANAUX_COLLECTE("Canaux de collecte des données", new Cible("traitement", "canaux_collecte_donnees")),
    LICEITE("Licéité du traitement", new Cible("traitement", "liceite_traitement")),
    RECOURS_AUTOMATISE("Recours au traitement automatisé (y compris profilage)",
            new Cible("traitement", "recours_traitements_automatises")),
    DISPOSITIONS_SECURITE("Dispositions existantes pour assurer la sécurité des données",
            new Cible("traitement", "dispositions_securite_donnees_physique"),
            new Cible("traitement", "dispositions_securite_donnees_numerique")),
    DUREE_ARCHIVAGE_COURANT("Durée d'archivage courant", new Cible("traitement", "duree_conservation")),
    DUREE_ARCHIVAGE_DEFINITIF("Durée d'archivage définitif", new Cible("traitement", "duree_archivage")),
    CATEGORIES_DESTINATAIRES("Catégories de destinataires", new Cible("traitement", "categories_destinataires")),
    RAISONS_TRANSFERT("Raisons du transfert vers les catégories de destinataires",
            new Cible("traitement", "raisons_transfert_destinataires")),
    TRANSFERTS_HORS_UE("Transferts hors UE", new Cible("traitement", "transferts_hors_ue")),
    PAYS_DESTINATAIRES("Pays destinataires", new Cible("traitement", "pays_destinataires"));

    private final String libelle;
    private final List<Cible> cibles;

    CorrespondanceChampRegistre(String libelle, Cible... cibles) {
        this.libelle = libelle;
        this.cibles = List.of(cibles);
    }

    public String libelle() {
        return libelle;
    }

    public List<Cible> cibles() {
        return cibles;
    }

    /**
     * Résout les cibles BDD d'un libellé de l'onglet FR_Définitions.
     *
     * @return les cibles associées, ou vide si le libellé est inconnu
     *         (nouveau champ d'une édition future, ou champ sans correspondance)
     */
    public static Optional<List<Cible>> resoudre(String libelle) {
        return Arrays.stream(values())
                .filter(correspondance -> correspondance.libelle.equals(libelle))
                .findFirst()
                .map(CorrespondanceChampRegistre::cibles);
    }

    /**
     * Cible d'un champ dans le modèle de données.
     *
     * @param tableCible   table correspondante (nulle si le champ n'a pas de traduction en base)
     * @param colonneCible colonne correspondante (nulle si le champ porte sur une relation
     *                     ou n'a pas de traduction en base)
     */
    public record Cible(String tableCible, String colonneCible) {
    }
}
