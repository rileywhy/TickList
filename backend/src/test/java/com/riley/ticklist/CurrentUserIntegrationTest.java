package com.riley.ticklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.riley.ticklist.support.ApiTestClient;

/**
 * Contract tests for GET /current_user, the session-rehydration endpoint (M9).
 *
 * The frontend depends on this exact contract after a page refresh: it trades the persisted JWT
 * for the user's identity. A 401 here is the signal that drives the frontend's cleanup path
 * (delete stored token, show login), so the status codes are pinned exactly -- the same lesson
 * as H3: is4xxClientError() would also pass under a regression back to 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CurrentUserIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TickRepository tickRepository;
    @Autowired private UserRepository userRepository;

    /** The signing secret the app is running with, so tests can mint tokens it will accept. */
    @Value("${security.jwt.secret}") private String jwtSecret;

    private ApiTestClient api;

    private String aliceToken;

    @BeforeEach
    void setUp() throws Exception {
        tickRepository.deleteAll();
        userRepository.deleteAll();

        api = new ApiTestClient(mockMvc);

        api.register("alice@example.com", "Alice", "Anchor", "alice-pass");
        api.register("bob@example.com", "Bob", "Belay", "bob-pass");
        aliceToken = api.login("alice@example.com", "alice-pass");
    }

    @Test
    @DisplayName("No token is 401, the signal the frontend's rehydration cleanup relies on")
    void currentUserWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/current_user"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An expired token is 401, not 403")
    void currentUserWithExpiredTokenIsUnauthorized() throws Exception {
        User alice = userRepository.findByEmail("alice@example.com");

        // Correctly signed with the running secret, but a negative lifetime means it is already
        // expired when minted -- no sleeping required to reach the expiry path.
        String expiredToken = new JwtService(jwtSecret, -1000L).generateToken(alice);

        mockMvc.perform(get("/current_user").header("Authorization", api.bearer(expiredToken)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Identity comes from the token: Alice's token returns Alice, exactly")
    void currentUserReturnsTokenOwnersIdentity() throws Exception {
        mockMvc.perform(get("/current_user").header("Authorization", api.bearer(aliceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andExpect(jsonPath("$.firstName").value("Alice"))
            .andExpect(jsonPath("$.lastName").value("Anchor"));
    }

    @Test
    @DisplayName("The response never carries the password hash or entity internals")
    void currentUserResponseContainsNoSensitiveFields() throws Exception {
        // Pins the DTO boundary: CurrentUserResponse, not the User entity. If someone "simplifies"
        // the endpoint back to returning User, the bcrypt hash ships to the browser and this fails.
        mockMvc.perform(get("/current_user").header("Authorization", api.bearer(aliceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.id").doesNotExist());
    }
}
