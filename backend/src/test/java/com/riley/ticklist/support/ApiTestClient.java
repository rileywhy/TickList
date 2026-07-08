package com.riley.ticklist.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test-only client that drives the TickList HTTP API through MockMvc.
 *
 * <p>Extracted from the integration tests so any future {@code @SpringBootTest} can register
 * users, log in for a JWT, and create ticks without re-implementing the plumbing. Construct one
 * per test from the autowired MockMvc: {@code ApiTestClient api = new ApiTestClient(mockMvc);}
 *
 * <p>These methods perform <em>arrange</em> (setup) actions, so they assert their own happy path
 * (register/login/create must return 200). The behaviour a test is actually <em>verifying</em>
 * should still be driven with {@code mockMvc.perform(...)} directly in the test, so the
 * request-under-test stays visible in the test body.
 */
public class ApiTestClient {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiTestClient(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /** Registers a user via POST /register (the real endpoint bcrypt-hashes the password). */
    public void register(String email, String firstName, String lastName, String rawPassword) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("email", email);
        body.put("password", rawPassword);

        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
            .andExpect(status().isOk());
    }

    /** Logs in via POST /login and returns the raw JWT for use as a bearer token. */
    public String login(String email, String rawPassword) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("email", email);
        body.put("password", rawPassword);

        MvcResult result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
            .andExpect(status().isOk())
            .andReturn();

        return readJson(result).get("token").asText();
    }

    /** Creates a tick via POST /ticks as the given user and returns its generated id. */
    public Long createTick(String token, String climbName) throws Exception {
        MvcResult result = mockMvc.perform(post("/ticks")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(tickBody(climbName).toString()))
            .andExpect(status().isOk())
            .andReturn();

        return readJson(result).get("id").asLong();
    }

    /** A minimal valid tick payload (climbName is @NotBlank on the entity). */
    public ObjectNode tickBody(String climbName) {
        ObjectNode tick = objectMapper.createObjectNode();
        tick.put("climbName", climbName);
        tick.put("grade", "5.11a");
        tick.put("tickType", "SEND");
        return tick;
    }

    /** A nested {user} reference, for tests that try to spoof ownership via the request body. */
    public ObjectNode userRef(Long id, String email) {
        ObjectNode user = objectMapper.createObjectNode();
        user.put("id", id);
        user.put("email", email);
        return user;
    }

    /** A two-row Mountain Project CSV (one sport route, one boulder) as a multipart upload. */
    public MockMultipartFile mountainProjectCsvUpload() {
        String header = String.join(",",
            "Date", "Route", "Rating", "Notes", "URL", "Pitches", "Location",
            "Avg Stars", "Your Stars", "Style", "Lead Style", "Route Type",
            "Your Rating", "Length", "Rating Code");
        String sportRow = "2026-06-15,Imported One,5.10a,,https://mp.com/1,1,Eldo,4,3,Lead,Redpoint,Trad,5.10a,80,";
        String boulderRow = "2026-06-16,Imported Two,V3,,https://mp.com/2,1,Bishop,4,3,Send,,Boulder,,,";
        String csv = String.join("\n", header, sportRow, boulderRow) + "\n";

        return new MockMultipartFile("file", "ticks.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
    }

    /** Parses a MockMvc response body as a JSON tree. */
    public JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Formats a value for the Authorization header: "Bearer &lt;token&gt;". */
    public String bearer(String token) {
        return "Bearer " + token;
    }
}
