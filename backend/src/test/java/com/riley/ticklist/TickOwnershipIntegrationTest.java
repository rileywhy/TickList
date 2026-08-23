package com.riley.ticklist;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.riley.ticklist.support.ApiTestClient;

/**
 * End-to-end ownership / IDOR regression tests for the tick endpoints.
 *
 * Every request runs through the REAL Spring Security filter chain
 * (JwtAuthenticationFilter -> SecurityContext -> controller -> repository -> H2),
 * so these prove the actual runtime behaviour rather than a mocked stand-in. 
 *
 * Two real users are registered and logged in for genuine JWTs (see {@link ApiTestClient}).
 * The recurring shape: Alice owns a tick, Bob is a valid logged-in user, and Bob must never
 * be able to see or mutate Alice's data -- the correct answer is 404 (not 403, which would
 * leak the tick's existence), never 200.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TickOwnershipIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TickRepository tickRepository;
    @Autowired private ImportBatchRepository importBatchRepository;
    @Autowired private SkippedRowRepository skippedRowRepository;
    @Autowired private UserRepository userRepository;

    /** The signing secret the app is running with, so tests can mint tokens it will accept. */
    @Value("${security.jwt.secret}") private String jwtSecret;

    private ApiTestClient api;

    private String aliceToken;
    private String bobToken;
    private Long bobId;

    @BeforeEach
    void setUp() throws Exception {
        // The schema is created once per context; clear rows between tests for isolation.
        // Ticks first (they hold the FK to app_users), then users.
        skippedRowRepository.deleteAll();
        tickRepository.deleteAll();
        importBatchRepository.deleteAll();
        userRepository.deleteAll();

        api = new ApiTestClient(mockMvc);

        api.register("alice@example.com", "Alice", "Ascent", "alice-pw");
        api.register("bob@example.com", "Bob", "Belay", "bob-pass");

        aliceToken = api.login("alice@example.com", "alice-pw");
        bobToken = api.login("bob@example.com", "bob-pass");
        bobId = userRepository.findByEmail("bob@example.com").getId();
    }

    // ---------- Item 1: per-user authorization on the tick endpoints ----------

    @Test
    @DisplayName("GET /ticks returns only the caller's own ticks")
    void listTicksIsScopedToCaller() throws Exception {
        api.createTick(aliceToken, "Alice Arete");
        api.createTick(bobToken, "Bob Boulder");

        mockMvc.perform(get("/ticks").header("Authorization", api.bearer(aliceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].climbName").value("Alice Arete"));

        mockMvc.perform(get("/ticks").header("Authorization", api.bearer(bobToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].climbName").value("Bob Boulder"));
    }

    @Test
    @DisplayName("GET /ticks/{id} returns 404 when the caller does not own the tick")
    void getForeignTickReturns404() throws Exception {
        Long aliceTickId = api.createTick(aliceToken, "Alice Arete");

        // Owner can read it...
        mockMvc.perform(get("/ticks/" + aliceTickId).header("Authorization", api.bearer(aliceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.climbName").value("Alice Arete"));

        // ...a different, valid, logged-in user cannot (404, no existence leak).
        mockMvc.perform(get("/ticks/" + aliceTickId).header("Authorization", api.bearer(bobToken)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /ticks/{id} returns 404 and does not mutate a tick the caller does not own")
    void updateForeignTickReturns404AndDoesNotMutate() throws Exception {
        Long aliceTickId = api.createTick(aliceToken, "Alice Arete");

        mockMvc.perform(put("/ticks/" + aliceTickId)
                .header("Authorization", api.bearer(bobToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.tickBody("HACKED").toString()))
            .andExpect(status().isNotFound());

        Tick unchanged = tickRepository.findById(aliceTickId).orElseThrow();
        assertThat(unchanged.getClimbName()).isEqualTo("Alice Arete");
        assertThat(unchanged.getUser().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("DELETE /ticks/{id} returns 404 and does not delete a tick the caller does not own")
    void deleteForeignTickReturns404AndDoesNotDelete() throws Exception {
        Long aliceTickId = api.createTick(aliceToken, "Alice Arete");

        mockMvc.perform(delete("/ticks/" + aliceTickId).header("Authorization", api.bearer(bobToken)))
            .andExpect(status().isNotFound());

        assertThat(tickRepository.findById(aliceTickId)).isPresent();
    }

    @Test
    @DisplayName("POST /ticks binds the owner to the token, ignoring a 'user' in the request body")
    void createTickIgnoresBodySuppliedOwner() throws Exception {
        // Alice creates a tick whose body tries to hand ownership to Bob.
        ObjectNode body = api.tickBody("Alice Arete");
        body.set("user", api.userRef(bobId, "bob@example.com"));

        mockMvc.perform(post("/ticks")
                .header("Authorization", api.bearer(aliceToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
            .andExpect(status().isOk());

        // The persisted tick belongs to Alice (the token), not Bob (the body).
        assertThat(tickRepository.findByUser(userRepository.findByEmail("bob@example.com"))).isEmpty();
        List<Tick> aliceTicks = tickRepository.findByUser(userRepository.findByEmail("alice@example.com"));
        assertThat(aliceTicks).hasSize(1);
        assertThat(aliceTicks.get(0).getUser().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("POST /ticks cannot overwrite another user's tick by supplying its id")
    void createTickCannotHijackForeignTickById() throws Exception {
        // Alice owns a tick carrying fields a JPA merge would silently null out.
        ObjectNode aliceBody = api.tickBody("Alice Arete");
        aliceBody.put("location", "Eldorado Canyon");
        aliceBody.put("notes", "Best pitch of the season.");

        MvcResult created = mockMvc.perform(post("/ticks")
                .header("Authorization", api.bearer(aliceToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aliceBody.toString()))
            .andExpect(status().isOk())
            .andReturn();
        Long aliceTickId = api.readJson(created).get("id").asLong();

        // Bob POSTs *Alice's* id. TickRequest has no id field, so the smuggled value has
        // nowhere to land: the request succeeds as an ordinary create for Bob. (Before the
        // DTO, this same body reached save() as a merge and only a hand-written id guard
        // stood in the way.)
        ObjectNode hijack = api.tickBody("pwned");
        hijack.put("id", aliceTickId);

        MvcResult hijackResult = mockMvc.perform(post("/ticks")
                .header("Authorization", api.bearer(bobToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(hijack.toString()))
            .andExpect(status().isOk())
            .andReturn();

        // Alice's row is untouched: still hers, and every field survives.
        Tick unchanged = tickRepository.findById(aliceTickId).orElseThrow();
        assertThat(unchanged.getUser().getEmail()).isEqualTo("alice@example.com");
        assertThat(unchanged.getClimbName()).isEqualTo("Alice Arete");
        assertThat(unchanged.getLocation()).isEqualTo("Eldorado Canyon");
        assertThat(unchanged.getNotes()).isEqualTo("Best pitch of the season.");

        // Bob got a brand-new row of his own under a different id -- an INSERT, not a merge.
        Long bobTickId = api.readJson(hijackResult).get("id").asLong();
        assertThat(bobTickId).isNotEqualTo(aliceTickId);
        List<Tick> bobTicks = tickRepository.findByUser(userRepository.findByEmail("bob@example.com"));
        assertThat(bobTicks).hasSize(1);
        assertThat(bobTicks.get(0).getClimbName()).isEqualTo("pwned");
        assertThat(tickRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Tick responses expose only the TickResponse contract, never entity internals")
    void tickResponsesOmitServerInternals() throws Exception {
        Long tickId = api.createTick(aliceToken, "Contract Crack");

        // The response shape is TickResponse's choice, not the Tick entity's: the owning
        // user (and their bcrypt hash) plus normalization internals must never serialize.
        mockMvc.perform(get("/ticks/" + tickId).header("Authorization", api.bearer(aliceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(tickId))
            .andExpect(jsonPath("$.climbName").value("Contract Crack"))
            .andExpect(jsonPath("$.user").doesNotExist())
            .andExpect(jsonPath("$.rawGrade").doesNotExist())
            .andExpect(jsonPath("$.personalGrade").doesNotExist());
    }

    @Test
    @DisplayName("POST /ticks resolves a CSV grade mapping and exposes difficultyScore")
    void createTickExposesDifficultyScore() throws Exception {
        ObjectNode body = api.tickBody("Mapped Arete");
        body.put("grade", "5.11a");
        body.put("discipline", "TRAD");

        mockMvc.perform(post("/ticks")
                .header("Authorization", api.bearer(aliceToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gradeSystem").value("YDS"))
            .andExpect(jsonPath("$.gradeValue").value(11.0))
            .andExpect(jsonPath("$.difficultyScore").value(55.0));
    }

    @Test
    @DisplayName("PUT /ticks/{id} cannot reassign ownership via the request body")
    void updateCannotReassignOwner() throws Exception {
        Long aliceTickId = api.createTick(aliceToken, "Alice Arete");

        ObjectNode body = api.tickBody("Alice Arete v2");
        body.set("user", api.userRef(bobId, "bob@example.com"));

        mockMvc.perform(put("/ticks/" + aliceTickId)
                .header("Authorization", api.bearer(aliceToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
            .andExpect(status().isOk());

        Tick updated = tickRepository.findById(aliceTickId).orElseThrow();
        assertThat(updated.getClimbName()).isEqualTo("Alice Arete v2");        // data field did update
        assertThat(updated.getUser().getEmail()).isEqualTo("alice@example.com"); // owner did NOT change
    }

    @Test
    @DisplayName("Tick endpoints reject unauthenticated requests with 401")
    void tickEndpointsRequireAuthentication() throws Exception {
        // No Authorization header -> blocked by SecurityConfig before reaching the controller.
        // 401 specifically, not merely "some 4xx": the frontend redirects to login on 401 and
        // only shows an error banner on 403, so the exact status is the contract.
        mockMvc.perform(get("/ticks"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An expired token is rejected with 401, so the client knows to re-authenticate")
    void expiredTokenIsUnauthorized() throws Exception {
        User alice = userRepository.findByEmail("alice@example.com");

        // Correctly signed with the running secret, but a negative lifetime means it is already
        // expired when minted -- no sleeping required to reach the expiry path.
        String expiredToken = new JwtService(jwtSecret, -1000L).generateToken(alice);

        // JwtAuthenticationFilter catches the ExpiredJwtException and continues the chain with no
        // principal, so this arrives at the authorization check as anonymous. That is an identity
        // failure (401), not a permissions failure (403).
        mockMvc.perform(get("/ticks").header("Authorization", api.bearer(expiredToken)))
            .andExpect(status().isUnauthorized());
    }

    // ---------- Item 2: imported ticks are bound to the authenticated importer ----------

    @Test
    @DisplayName("Import binds every imported tick to the authenticated user")
    void importBindsTicksToAuthenticatedUser() throws Exception {
        mockMvc.perform(multipart("/imports/mountain-project")
                .file(api.mountainProjectCsvUpload())
                .header("Authorization", api.bearer(aliceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.importedRows").value(2));

        User alice = userRepository.findByEmail("alice@example.com");
        User bob = userRepository.findByEmail("bob@example.com");

        List<Tick> aliceTicks = tickRepository.findByUser(alice);
        assertThat(aliceTicks).hasSize(2);
        assertThat(aliceTicks).allSatisfy(tick ->
            assertThat(tick.getUser().getEmail()).isEqualTo("alice@example.com"));

        // Nothing leaks to another user, and no imported tick is left ownerless.
        assertThat(tickRepository.findByUser(bob)).isEmpty();
        assertThat(tickRepository.findAll()).allSatisfy(tick ->
            assertThat(tick.getUser()).isNotNull());

        // Bob's own list stays empty.
        mockMvc.perform(get("/ticks").header("Authorization", api.bearer(bobToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Import rejects unauthenticated requests and saves nothing")
    void importRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/imports/mountain-project").file(api.mountainProjectCsvUpload()))
            .andExpect(status().isUnauthorized());

        assertThat(tickRepository.findAll()).isEmpty();
    }
}
