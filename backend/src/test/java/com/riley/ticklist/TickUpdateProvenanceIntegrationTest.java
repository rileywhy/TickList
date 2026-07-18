package com.riley.ticklist;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.riley.ticklist.support.ApiTestClient;

/**
 * Regression tests for import provenance surviving a UI edit.
 *
 * <p><b>The bug these pin.</b> {@code PUT /ticks/{id}} used to copy {@code rawGrade} and
 * {@code climbHeight} out of the request body unconditionally. No client ever sends those two
 * fields -- the frontend's {@code toTickPayload} (tickConfig.ts) builds a body from the edit
 * form, which has no input for either -- so they arrived null on every edit and were written
 * straight over the imported values. {@code climbHeight} was simply destroyed. {@code rawGrade}
 * was worse: {@code GradeMappingService.applyGradeMapping} backfills a blank {@code rawGrade}
 * from the display grade, so instead of going null it was silently rewritten to whatever the
 * user had just typed -- a column whose entire job is to record "this is the literal string the
 * import gave us" ended up asserting something the import never said.
 *
 * <p><b>Why that matters beyond tidiness.</b> The planned import idempotency work derives a
 * deterministic {@code externalId} from raw imported values. A {@code rawGrade} quietly rewritten
 * by an unrelated edit produces a different dedup key, so a re-import duplicates the tick. The
 * corruption is invisible today and only detonates once the thing that depends on it exists.
 *
 * <p><b>The fix</b> was to stop copying both fields in {@code TickController.updateTick}: they are
 * import-only provenance, not user-editable data. (A user-supplied height correction is a separate,
 * later feature -- and belongs on a Climb, not on one person's tick.)
 *
 * <p><b>Reading these tests.</b> The scenario is always: import a real Mountain Project CSV, then
 * PUT a frontend-shaped body (one that omits rawGrade and climbHeight, exactly as the real client
 * does) and assert the imported values survive. Note the edit must <em>change the grade</em> to be
 * meaningful: the importer stores {@code grade} and {@code rawGrade} as the same string, so an edit
 * that leaves the grade alone would pass even with the bug present, because backfilling rawGrade
 * from grade would reproduce the identical value. If you ever "simplify" these tests by dropping
 * the grade change, they stop testing anything.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TickUpdateProvenanceIntegrationTest {

    /** The sport/trad row in {@link ApiTestClient#mountainProjectCsvUpload()}: 5.10a, 80ft, Trad. */
    private static final String IMPORTED_CLIMB = "Imported One";
    private static final String IMPORTED_RAW_GRADE = "5.10a";
    private static final double IMPORTED_CLIMB_HEIGHT = 80.0;

    /** What the user retypes into the grade field. Must differ from IMPORTED_RAW_GRADE (see class javadoc). */
    private static final String EDITED_GRADE = "5.11a";

    @Autowired private MockMvc mockMvc;
    @Autowired private TickRepository tickRepository;
    @Autowired private UserRepository userRepository;

    private ApiTestClient api;
    private String aliceToken;

    @BeforeEach
    void setUp() throws Exception {
        // The schema is created once per context; clear rows between tests for isolation.
        // Ticks first (they hold the FK to app_users), then users.
        tickRepository.deleteAll();
        userRepository.deleteAll();

        api = new ApiTestClient(mockMvc);
        api.register("alice@example.com", "Alice", "Ascent", "alice-pw");
        aliceToken = api.login("alice@example.com", "alice-pw");
    }

    @Test
    @DisplayName("Editing an imported tick's grade preserves the raw grade the import recorded")
    void updatePreservesImportedRawGrade() throws Exception {
        Tick imported = importAndFindSportRow();
        assertThat(imported.getRawGrade()).isEqualTo(IMPORTED_RAW_GRADE);

        editGrade(imported.getId(), EDITED_GRADE);

        Tick edited = tickRepository.findById(imported.getId()).orElseThrow();
        assertThat(edited.getGrade()).isEqualTo(EDITED_GRADE);            // the edit did apply...
        assertThat(edited.getRawGrade()).isEqualTo(IMPORTED_RAW_GRADE);   // ...without rewriting provenance
    }

    @Test
    @DisplayName("Editing an imported tick preserves the climb height the import recorded")
    void updatePreservesImportedClimbHeight() throws Exception {
        Tick imported = importAndFindSportRow();
        assertThat(imported.getClimbHeight()).isEqualTo(IMPORTED_CLIMB_HEIGHT);

        editGrade(imported.getId(), EDITED_GRADE);

        Tick edited = tickRepository.findById(imported.getId()).orElseThrow();
        assertThat(edited.getClimbHeight()).isEqualTo(IMPORTED_CLIMB_HEIGHT);
    }

    @Test
    @DisplayName("A preserved raw grade does not hijack the mapping: the edited grade drives the score")
    void editedGradeDrivesTheMappingNotThePreservedRawGrade() throws Exception {
        Tick imported = importAndFindSportRow();
        Double importedScore = imported.getDifficultyScore();
        assertThat(importedScore).isNotNull();

        editGrade(imported.getId(), EDITED_GRADE);

        // applyGradeMapping resolves from `grade` first and falls back to `rawGrade`. Now that
        // rawGrade survives an edit, this guards the other half of that contract: the stale raw
        // value must not win the lookup and leave the tick displaying 5.11a while scored as 5.10a.
        // Asserted as a relationship rather than a literal, because difficultyScore is a tuned
        // opinion in grade-mappings.csv and a legitimate retune should not break this test.
        Tick edited = tickRepository.findById(imported.getId()).orElseThrow();
        assertThat(edited.getGradeValue()).isEqualTo(11.0);
        assertThat(edited.getDifficultyScore()).isGreaterThan(importedScore);
    }

    // ---------- helpers ----------

    /** Imports the two-row MP fixture as Alice and returns the persisted sport/trad row. */
    private Tick importAndFindSportRow() throws Exception {
        mockMvc.perform(multipart("/imports/mountain-project")
                .file(api.mountainProjectCsvUpload())
                .header("Authorization", api.bearer(aliceToken)))
            .andExpect(status().isOk());

        List<Tick> ticks = tickRepository.findByUser(userRepository.findByEmail("alice@example.com"));
        return ticks.stream()
            .filter(tick -> IMPORTED_CLIMB.equals(tick.getClimbName()))
            .findFirst()
            .orElseThrow();
    }

    /**
     * PUTs a frontend-shaped edit that changes only the grade.
     *
     * <p>The body deliberately mirrors what {@code toTickPayload} actually sends -- notably it
     * omits {@code rawGrade} and {@code climbHeight}. Do not add them here: their absence is the
     * whole point of these tests.
     */
    private void editGrade(Long tickId, String newGrade) throws Exception {
        ObjectNode body = api.tickBody(IMPORTED_CLIMB);
        body.put("grade", newGrade);
        body.put("discipline", "TRAD");

        mockMvc.perform(put("/ticks/" + tickId)
                .header("Authorization", api.bearer(aliceToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
            .andExpect(status().isOk());
    }
}
