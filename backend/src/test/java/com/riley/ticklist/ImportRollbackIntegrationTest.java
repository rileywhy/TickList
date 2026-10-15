package com.riley.ticklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.io.StringReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Pins the all-or-nothing behaviour of {@link Importer#importCSV}.
 *
 * <p>The unit tests in {@link ImporterTest} mock every repository, so they cannot see
 * whether a transaction exists. This test boots the real context against H2, lets the
 * first row save normally, then makes the second save fail the way a dropped connection
 * would. With {@code @Transactional} in place the first tick and the batch row roll back
 * with it; without it they stay behind, which is the bug this test exists to catch.
 */
@SpringBootTest
@ActiveProfiles("test")
class ImportRollbackIntegrationTest {

    private static final String KAYA_HEADER =
        "date,stiffness,rating,ascent_type,attempts,grade,color,climb_name,gym,location,country";

    @Autowired private Importer importer;
    @MockitoSpyBean private TickRepository tickRepository;
    @Autowired private ImportBatchRepository importBatchRepository;
    @Autowired private SkippedRowRepository skippedRowRepository;
    @Autowired private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        skippedRowRepository.deleteAll();
        tickRepository.deleteAll();
        importBatchRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setFirstName("Alice");
        user.setLastName("Ascent");
        user.setEmail("alice@example.com");
        user.setPassword("not-a-real-hash");
        user = userRepository.save(user);
    }

    @Test
    @DisplayName("A failure partway through an import leaves no ticks and no batch row behind")
    void failureMidImportLeavesNoTicksAndNoBatch() {
        // Row one passes its duplicate check and saves for real. Row two's duplicate check
        // throws the way a dropped connection would. The check runs after the per-row catch
        // block, so the exception escapes importCSV like a real database failure.
        doReturn(false)
            .doThrow(new DataAccessResourceFailureException("connection dropped"))
            .when(tickRepository).existsByUserAndSourceAppAndExternalId(any(User.class), any(SourceApp.class), anyString());

        String csv = KAYA_HEADER + "\n"
            + "Sun Mar 26 2023 16:30:56 GMT+0000 (GMT+00:00),0,,Flash,1,v2,Pink,,The Proving Ground,,\n"
            + "Sun Mar 26 2023 16:45:10 GMT+0000 (GMT+00:00),0,,Send,2,v3,Blue,,The Proving Ground,,\n"
            + "Sun Mar 26 2023 17:02:33 GMT+0000 (GMT+00:00),0,,Flash,1,v1,Green,,The Proving Ground,,\n";

        assertThatThrownBy(() -> importer.importCSV(new StringReader(csv), user, "kaya.csv"))
            .isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(tickRepository.count()).isZero();
        assertThat(importBatchRepository.count()).isZero();
    }
}
