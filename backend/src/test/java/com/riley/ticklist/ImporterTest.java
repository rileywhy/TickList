package com.riley.ticklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;

@Execution(ExecutionMode.SAME_THREAD)
class ImporterTest {
    private static final String HEADER = String.join(",",
        "Date",
        "Route",
        "Rating",
        "Notes",
        "URL",
        "Pitches",
        "Location",
        "Avg Stars",
        "Your Stars",
        "Style",
        "Lead Style",
        "Route Type",
        "Your Rating",
        "Length",
        "Rating Code"
    );

    @TempDir
    private Path tempDir;

    private Path testCsv;
    private SendRepository sendRepository;
    private Importer importer;

    @BeforeEach
    void setUp() {
        testCsv = tempDir.resolve("ticks.csv");
        sendRepository = mock(SendRepository.class);
        importer = new Importer(sendRepository);
    }

    @Test
    void importsCompleteCsvRowIntoSend() throws Exception {
        writeTicksCsv(
            "2026-06-15,The Bulge,5.10a,\"Great movement, bad feet\",https://mountainproject.com/route/123,1,Eldorado Canyon,4.5,3,Lead,Redpoint,Trad,5.10b,80,"
        );

        importer.importCSV(testCsv);

        ArgumentCaptor<Send> sendCaptor = ArgumentCaptor.forClass(Send.class);
        verify(sendRepository).save(sendCaptor.capture());

        Send send = sendCaptor.getValue();
        assertThat(send.getSendDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(send.getClimbName()).isEqualTo("The Bulge");
        assertThat(send.getGrade()).isEqualTo("5.10a");
        assertThat(send.getNotes()).isEqualTo("Great movement, bad feet");
        assertThat(send.getSourceUrl()).isEqualTo("https://mountainproject.com/route/123");
        assertThat(send.getPitches()).isEqualTo(1);
        assertThat(send.getLocation()).isEqualTo("Eldorado Canyon");
        assertThat(send.getStars()).isEqualTo(4.5);
        assertThat(send.getUserStars()).isEqualTo(3.0);
        assertThat(send.getStyle()).isEqualTo("Lead");
        assertThat(send.getRopeSendStyle()).isEqualTo(RopeSendStyle.REDPOINT);
        assertThat(send.getDiscipline()).isEqualTo(Discipline.TRAD);
        assertThat(send.getPersonalGrade()).isEqualTo("5.10b");
        assertThat(send.getClimbHeight()).isEqualTo(80.0);
    }

    @Test
    void importsBlankNumericColumnsAsNull() throws Exception {
        writeTicksCsv(
            "2026-06-15,The Bulge,5.10a,Fun route,https://mountainproject.com/route/123,,Eldorado Canyon,,,Lead,Redpoint,Trad,5.10b,,"
        );

        importer.importCSV(testCsv);

        ArgumentCaptor<Send> sendCaptor = ArgumentCaptor.forClass(Send.class);
        verify(sendRepository).save(sendCaptor.capture());

        Send send = sendCaptor.getValue();
        assertThat(send.getPitches()).isNull();
        assertThat(send.getStars()).isNull();
        assertThat(send.getUserStars()).isNull();
        assertThat(send.getClimbHeight()).isNull();
    }

    @Test
    void failsWithoutSavingWhenTicksCsvIsMissing() throws IOException {
        Files.deleteIfExists(testCsv);

        assertThatThrownBy(() -> importer.importCSV(testCsv))
            .isInstanceOf(NoSuchFileException.class);

        verify(sendRepository, never()).save(any(Send.class));
    }

    @Test
    void failsWithoutSavingWhenNumericColumnsAreMalformed() throws IOException {
        writeTicksCsv(
            "2026-06-15,The Bulge,5.10a,Fun route,https://mountainproject.com/route/123,one,Eldorado Canyon,4,3,FLASH,REDPOINT,Trad,5.10b,80,"
        );

        assertThatThrownBy(() -> importer.importCSV(testCsv))
            .isInstanceOf(NumberFormatException.class);

        verify(sendRepository, never()).save(any(Send.class));
    }

    @Test
    void importsBlankLeadStyleAsUnknownRopeSendStyle() throws Exception {
        writeTicksCsv(
            "2026-06-15,Hi-C,V1,Fun boulder,https://mountainproject.com/route/123,1,Eldorado Canyon,4,3,Send,,Boulder,,,"
        );

        importer.importCSV(testCsv);

        ArgumentCaptor<Send> sendCaptor = ArgumentCaptor.forClass(Send.class);
        verify(sendRepository).save(sendCaptor.capture());

        assertThat(sendCaptor.getValue().getRopeSendStyle()).isEqualTo(RopeSendStyle.UNKNOWN);
    }

    @Test
    void failsWithoutSavingWhenDateFormatIsUnsupported() throws IOException {
        writeTicksCsv(
            "not-a-date,The Bulge,5.10a,Fun route,https://mountainproject.com/route/123,1,Eldorado Canyon,4,3,FLASH,REDPOINT,Trad,5.10b,80,"
        );

        assertThatThrownBy(() -> importer.importCSV(testCsv))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported date format");

        verify(sendRepository, never()).save(any(Send.class));
    }
    // main test method to run the importer against the actual inputs/ticks.csv file for inspection
    @Test
    @EnabledIfSystemProperty(named = "importer.inspectInputs", matches = "true")
    void fullCSVTest() throws Exception {
        List<Send> savedSends = new ArrayList<>();
        when(sendRepository.save(any(Send.class))).thenAnswer(invocation -> {
            Send send = invocation.getArgument(0);
            savedSends.add(send);
            printImportedSend(send);
            return send;
        });

        try {
            importer.importCSV();
        } catch (Exception error) {
            System.out.printf(
                "Importer failed while reading inputs/ticks.csv after %d saved send(s): %s: %s%n",
                savedSends.size(),
                error.getClass().getSimpleName(),
                error.getMessage()
            );
            throw error;
        }

        assertThat(savedSends).isNotEmpty();
    }

    private void writeTicksCsv(String... rows) throws IOException {
        Files.writeString(testCsv, HEADER + System.lineSeparator()
            + String.join(System.lineSeparator(), rows)
            + System.lineSeparator());
    }

    private void printImportedSend(Send send) {
        System.out.printf(
            "Imported send: climbName=%s, date=%s, grade=%s, discipline=%s, ropeSendStyle=%s, pitches=%s, stars=%s, userStars=%s, height=%s, url=%s%n",
            send.getClimbName(),
            send.getSendDate(),
            send.getGrade(),
            send.getDiscipline(),
            send.getRopeSendStyle(),
            send.getPitches(),
            send.getStars(),
            send.getUserStars(),
            send.getClimbHeight(),
            send.getSourceUrl()
        );
    }
}
