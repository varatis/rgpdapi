package com.minds.rgpd.infrastructure.keycloak;

import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.business.exceptions.IdentityProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

/**
 * Tests du client d'administration Keycloak : le serveur HTTP est simulé par
 * {@link MockRestServiceServer}, aucune requête réelle n'est émise. Valide le
 * flux client_credentials, la transmission du jeton, la pagination, la
 * relance après 401 et la lecture de l'en-tête Location.
 */
class KeycloakAdminClientTest {

    private static final String BASE = "http://keycloak:8080";
    private static final String TOKEN_URL = BASE + "/realms/minds-rgpd/protocol/openid-connect/token";
    private static final String USERS_URL = BASE + "/admin/realms/minds-rgpd/users";
    private static final String ALICE_ID = "d6dfd117-8047-4a9a-afca-f5268a38bfcf";
    private static final String BOB_ID = "6a04222b-60f8-434b-bdff-c01ce36fde2f";

    private KeycloakAdminClient client;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        KeycloakProperties properties = mock(KeycloakProperties.class);
        when(properties.getBaseUrl()).thenReturn(BASE);
        when(properties.getRealm()).thenReturn("minds-rgpd");
        when(properties.getAdminClientId()).thenReturn("minds-rgpd-admin");
        when(properties.getAdminClientSecret()).thenReturn("test-secret");
        when(properties.getPageSize()).thenReturn(2);

        client = new KeycloakAdminClient(properties, restTemplate);
    }

    private void attendreJeton(String jeton) {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of(
                        "grant_type", "client_credentials",
                        "client_id", "minds-rgpd-admin",
                        "client_secret", "test-secret")))
                .andRespond(withSuccess(
                        "{\"access_token\":\"" + jeton + "\",\"expires_in\":3600,\"scope\":\"profile\"}",
                        MediaType.APPLICATION_JSON));
    }

    /** Le jeton obtenu par client_credentials est transmis en Authorization sur chaque appel d'admin. */
    @Test
    void getUsersTransmetLeJetonEtParcourtLesPages() {
        attendreJeton("jwt-1");
        server.expect(requestTo(USERS_URL + "?first=0&max=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer jwt-1"))
                .andRespond(withSuccess("""
                        [
                          {"id":"%s","username":"alice@alpha.com","enabled":true,"notBefore":0},
                          {"id":"%s","username":"bob@alpha.com","enabled":false}
                        ]
                        """.formatted(ALICE_ID, BOB_ID), MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERS_URL + "?first=2&max=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<KeycloakUserRepresentation> users = client.getUsers();

        server.verify();
        assertThat(users).hasSize(2);
        assertThat(users.getFirst().getId()).isEqualTo(UUID.fromString(ALICE_ID));
        assertThat(users.getFirst().isEnabled()).isTrue();
        assertThat(users.get(1).isEnabled()).isFalse();
    }

    /** La recherche par e-mail interroge une liste et retient le premier résultat. */
    @Test
    void getUserByEmailRetientLePremierResultat() {
        attendreJeton("jwt-1");
        server.expect(requestTo(USERS_URL + "?email=alice%40alpha.com&exact=true&max=1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"id\":\"" + ALICE_ID + "\",\"username\":\"alice@alpha.com\",\"email\":\"alice@alpha.com\"}]",
                        MediaType.APPLICATION_JSON));

        Optional<KeycloakUserRepresentation> alice = client.getUserByEmail("alice@alpha.com");

        server.verify();
        assertThat(alice).isPresent();
        assertThat(alice.get().getEmail()).isEqualTo("alice@alpha.com");
    }

    /** Une réponse 401 déclenche le renouvellement du jeton puis une seconde tentative. */
    @Test
    void renouvelleLeJetonApresUn401() {
        attendreJeton("jwt-1");
        server.expect(requestTo(USERS_URL + "?first=0&max=2"))
                .andRespond(withUnauthorizedRequest());
        attendreJeton("jwt-2");
        server.expect(requestTo(USERS_URL + "?first=0&max=2"))
                .andExpect(header("Authorization", "Bearer jwt-2"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<KeycloakUserRepresentation> users = client.getUsers();

        server.verify();
        assertThat(users).isEmpty();
    }

    /**
     * Un jeton inaccessible doit remonter une erreur explicite (502 côté API)
     * plutôt qu'un échec silencieux qui masque la cause.
     */
    @Test
    void jetonInaccessibleLeveUneErreurExplicite() {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.getUsers())
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("jeton client_credentials");

        server.verify();
    }

    /**
     * Un identifiant déjà pris (409 « User exists with same username ») devient
     * un doublon explicite côté API — pas un 502 avec un indice « rôles »
     * qui égarerait le diagnostic.
     */
    @Test
    void createUserEnDoublonTraduitLeConflitKeycloak() {
        attendreJeton("jwt-1");
        server.expect(requestTo(USERS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .body("{\"errorMessage\":\"User exists with same username\"}"));

        assertThatThrownBy(() -> client.createUser(Map.of("username", "yo@yoann")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("yo@yoann");

        server.verify();
    }

    /**
     * Une écriture refusée (403 : compte de service sans rôles
     * realm-management) remonte une erreur explicite, pas un 500 brut —
     * l'indice « rôles realm-management » reste réservé aux refus
     * d'autorisation.
     */
    @Test
    void ecritureRefuseeLeveUneErreurExplicite() {
        attendreJeton("jwt-1");
        server.expect(requestTo(USERS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.createUser(Map.of("username", "alice@alpha.com")))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("realm-management");

        server.verify();
    }

    /** Un échec d'écriture sans rapport avec les permissions n'égare pas sur les rôles. */
    @Test
    void erreurDecritureSansRefusDAutorisationNeParlePasDeRoles() {
        attendreJeton("jwt-1");
        server.expect(requestTo(USERS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.createUser(Map.of("username", "alice@alpha.com")))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("appel Keycloak en échec")
                .hasMessageNotContaining("realm-management");

        server.verify();
    }

    /** Keycloak ne renvoie pas de corps à la création : l'UUID vient de l'en-tête Location. */
    @Test
    void createUserLitLIdentifiantDansLEnteteLocation() {
        attendreJeton("jwt-1");
        server.expect(requestTo(USERS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.username").value("alice@alpha.com"))
                .andRespond(withCreatedEntity(URI.create(USERS_URL + "/" + ALICE_ID)));

        UUID id = client.createUser(Map.of("username", "alice@alpha.com", "enabled", true));

        server.verify();
        assertThat(id).isEqualTo(UUID.fromString(ALICE_ID));
    }

    /** L'affectation de rôles envoie le tableau de représentations {"id","name"} attendu par Keycloak. */
    @Test
    void assignClientRolesEnvoieLesRepresentationsDeRoles() {
        String clientUuid = "9c1a1a48-1111-2222-3333-444455556666";
        String rolesUrl = BASE + "/admin/realms/minds-rgpd/clients/" + clientUuid + "/roles";
        String mappingUrl = USERS_URL + "/" + ALICE_ID + "/role-mappings/clients/" + clientUuid;

        attendreJeton("jwt-1");
        server.expect(requestTo(rolesUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {"id":"role-admin","name":"admin","description":"Administrateur"},
                          {"id":"role-user","name":"user","description":"Utilisateur"}
                        ]
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(mappingUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0].id").value("role-user"))
                .andExpect(jsonPath("$[0].name").value("user"))
                .andRespond(withSuccess());

        client.assignClientRoles(UUID.fromString(ALICE_ID), clientUuid, List.of("user"));

        server.verify();
    }

    /** Le mot de passe initial passe par reset-password, en temporaire. */
    @Test
    void resetPasswordEnvoieUnMotDePasseTemporaire() {
        attendreJeton("jwt-1");
        server.expect(requestTo(USERS_URL + "/" + ALICE_ID + "/reset-password"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.type").value("password"))
                .andExpect(jsonPath("$.value").value("MotDePasse123!"))
                .andExpect(jsonPath("$.temporary").value(true))
                .andRespond(withSuccess());

        client.resetPassword(UUID.fromString(ALICE_ID), "MotDePasse123!", true);

        server.verify();
    }

    /** La hiérarchie GET /groups imbrique les sous-groupes : seule lecture universelle. */
    @Test
    void getGroupHierarchyImbriqueLesSousGroupes() {
        attendreJeton("jwt-1");
        server.expect(requestTo(BASE + "/admin/realms/minds-rgpd/groups"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":"11111111-1111-1111-1111-111111111111","name":"clients","path":"/clients","subGroups":[
                          {"id":"22222222-2222-2222-2222-222222222222","name":"La breteche","path":"/clients/La breteche"}]}]
                        """, MediaType.APPLICATION_JSON));

        List<KeycloakGroupRepresentation> racine = client.getGroupHierarchy();

        server.verify();
        assertThat(racine).hasSize(1);
        assertThat(racine.getFirst().getSubGroups()).hasSize(1);
        assertThat(racine.getFirst().getSubGroups().getFirst().getName()).isEqualTo("La breteche");
    }

    /** Sous-groupes directs : endpoint des Keycloak récents, utilisé en repli de la hiérarchie. */
    @Test
    void getGroupChildrenListeLesSousGroupesDirects() {
        attendreJeton("jwt-1");
        server.expect(requestTo(BASE + "/admin/realms/minds-rgpd/groups/"
                + "11111111-1111-1111-1111-111111111111" + "/children"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":"22222222-2222-2222-2222-222222222222","name":"La breteche","path":"/clients/La breteche"}]
                        """, MediaType.APPLICATION_JSON));

        List<KeycloakGroupRepresentation> enfants =
                client.getGroupChildren(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        server.verify();
        assertThat(enfants).hasSize(1);
        assertThat(enfants.getFirst().getName()).isEqualTo("La breteche");
    }

    /**
     * Un 409 à la création d'un groupe (sous-groupe homonyme déjà présent,
     * parfois sous une casse ou avec des espaces différents) devient un
     * doublon explicite côté API.
     */
    @Test
    void createGroupEnDoublonTraduitLeConflitKeycloak() {
        attendreJeton("jwt-1");
        server.expect(requestTo(BASE + "/admin/realms/minds-rgpd/groups/"
                + "11111111-1111-1111-1111-111111111111" + "/children"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT).body("{\"error\":\"unknown_error\"}"));

        assertThatThrownBy(() -> client.createGroup("La breteche",
                UUID.fromString("11111111-1111-1111-1111-111111111111")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("La breteche");

        server.verify();
    }

    /** Un e-mail absent donne un Optional vide, pas d'exception. */
    @Test
    void getUserByEmailAbsentDonneOptionalVide() {
        attendreJeton("jwt-1");
        server.expect(requestTo(USERS_URL + "?email=inconnu%40alpha.com&exact=true&max=1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        Optional<KeycloakUserRepresentation> resultat = client.getUserByEmail("inconnu@alpha.com");

        server.verify();
        assertThat(resultat).isEmpty();
    }
}
