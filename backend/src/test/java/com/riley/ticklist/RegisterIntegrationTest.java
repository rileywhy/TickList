package com.riley.ticklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.riley.ticklist.support.ApiTestClient;

/**
 * Contract tests for POST /register after M4's boundary: the endpoint binds
 * RegisterRequest (not the User entity), validates with @Valid, answers 409 on
 * duplicates, and never serializes server-owned fields back out.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegisterIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TickRepository tickRepository;
    @Autowired private ImportBatchRepository importBatchRepository;
    @Autowired private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ApiTestClient api;

    @BeforeEach
    void setUp() throws Exception {
        tickRepository.deleteAll();
        importBatchRepository.deleteAll();
        userRepository.deleteAll();
        api = new ApiTestClient(mockMvc);
        api.register("alice@example.com", "Alice", "Anchor", "alice-pass");
    }

    private ObjectNode registerBody(String email, String firstName, String lastName, String password) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("email", email);
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("password", password);
        return body;
    }

    @Test
    @DisplayName("Happy path returns the DTO: identity fields only, no hash, no id")
    void registerReturnsIdentityWithoutServerOwnedFields() throws Exception {
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("bob@example.com", "Bob", "Belay", "bob-pass").toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("bob@example.com"))
            .andExpect(jsonPath("$.firstName").value("Bob"))
            .andExpect(jsonPath("$.lastName").value("Belay"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    @DisplayName("Blank email is a 400 naming the field, not a 500")
    void blankEmailIsRejectedWithFieldError() throws Exception {
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("", "Bob", "Belay", "bob-pass").toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.email").exists());
    }

    @Test
    @DisplayName("Malformed email is a 400")
    void malformedEmailIsRejected() throws Exception {
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("not-an-email", "Bob", "Belay", "bob-pass").toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.email").exists());
    }

    @Test
    @DisplayName("Short password is a 400 naming the field")
    void shortPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("bob@example.com", "Bob", "Belay", "short").toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.password").exists());
    }

    @Test
    @DisplayName("Duplicate email is a 409, and the existing account is untouched")
    void duplicateEmailIsConflict() throws Exception {
        String aliceHashBefore = userRepository.findByEmail("alice@example.com").getPassword();

        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("alice@example.com", "Mallory", "Menace", "mallory-pass").toString()))
            .andExpect(status().isConflict());

        User alice = userRepository.findByEmail("alice@example.com");
        assertThat(alice.getFirstName()).isEqualTo("Alice");
        assertThat(alice.getPassword()).isEqualTo(aliceHashBefore);
    }

    @Test
    @DisplayName("A body-supplied id cannot hijack an existing account (mass-assignment regression)")
    void bodySuppliedIdCannotOverwriteAnotherAccount() throws Exception {
        User alice = userRepository.findByEmail("alice@example.com");
        String aliceHashBefore = alice.getPassword();

        // The old entity-bound endpoint would have treated this as an UPDATE of
        // Alice's row. The DTO has no id field, so this key has nowhere to land.
        ObjectNode attack = registerBody("mallory@example.com", "Mallory", "Menace", "mallory-pass");
        attack.put("id", alice.getId());

        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(attack.toString()))
            .andExpect(status().isOk());

        User aliceAfter = userRepository.findByEmail("alice@example.com");
        assertThat(aliceAfter.getId()).isEqualTo(alice.getId());
        assertThat(aliceAfter.getFirstName()).isEqualTo("Alice");
        assertThat(aliceAfter.getPassword()).isEqualTo(aliceHashBefore);

        // Mallory got her own fresh account instead.
        User mallory = userRepository.findByEmail("mallory@example.com");
        assertThat(mallory).isNotNull();
        assertThat(mallory.getId()).isNotEqualTo(alice.getId());
    }
}
