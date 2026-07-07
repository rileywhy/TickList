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
    private TickRepository tickRepository;
    private Importer importer;
    private User importingUser;

    @BeforeEach
    void setUp() {
        testCsv = tempDir.resolve("ticks.csv");
        tickRepository = mock(TickRepository.class);
        importer = new Importer(tickRepository);
        importingUser = new User();
        importingUser.setId(1L);
        importingUser.setFirstName("Test");
        importingUser.setLastName("Climber");
        importingUser.setEmail("test@example.com");
        importingUser.setPassword("hashed-password");
    }

    @Test
    void importsCompleteCsvRowIntoTick() throws Exception {
        writeTicksCsv(
            "2026-06-15,The Bulge,5.10a,\"Great movement, bad feet\",https://mountainproject.com/route/123,1,Eldorado Canyon,4.5,3,Lead,Redpoint,Trad,5.10b,80,"
        );

        importer.importCSV(testCsv, importingUser);

        ArgumentCaptor<Tick> tickCaptor = ArgumentCaptor.forClass(Tick.class);
        verify(tickRepository).save(tickCaptor.capture());

        Tick tick = tickCaptor.getValue();
        assertThat(tick.getTickDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(tick.getClimbName()).isEqualTo("The Bulge");
        assertThat(tick.getGrade()).isEqualTo("5.10a");
        assertThat(tick.getRawGrade()).isEqualTo("5.10a");
        assertThat(tick.getGradeSystem()).isEqualTo(GradeSystem.YDS);
        assertThat(tick.getGradeValue()).isEqualTo(10.0);
        assertThat(tick.getNotes()).isEqualTo("Great movement, bad feet");
        assertThat(tick.getSourceUrl()).isEqualTo("https://mountainproject.com/route/123");
        assertThat(tick.getPitches()).isEqualTo(1);
        assertThat(tick.getLocation()).isEqualTo("Eldorado Canyon");
        assertThat(tick.getStars()).isEqualTo(4.5);
        assertThat(tick.getUserStars()).isEqualTo(3.0);
        assertThat(tick.getStyle()).isEqualTo("Lead");
        assertThat(tick.getRopeStyle()).isEqualTo(RopeStyle.REDPOINT);
        assertThat(tick.getTickType()).isEqualTo(TickType.SEND);
        assertThat(tick.getDiscipline()).isEqualTo(Discipline.TRAD);
        assertThat(tick.getPersonalGrade()).isEqualTo("5.10b");
        assertThat(tick.getClimbHeight()).isEqualTo(80.0);
        assertThat(tick.getUser()).isSameAs(importingUser);
    }

    @Test
    void importsBlankNumericColumnsAsNull() throws Exception {
        writeTicksCsv(
            "2026-06-15,The Bulge,5.10a,Fun route,https://mountainproject.com/route/123,,Eldorado Canyon,,,Lead,Redpoint,Trad,5.10b,,"
        );

        importer.importCSV(testCsv, importingUser);

        ArgumentCaptor<Tick> tickCaptor = ArgumentCaptor.forClass(Tick.class);
        verify(tickRepository).save(tickCaptor.capture());

        Tick tick = tickCaptor.getValue();
        assertThat(tick.getPitches()).isNull();
        assertThat(tick.getStars()).isNull();
        assertThat(tick.getUserStars()).isNull();
        assertThat(tick.getClimbHeight()).isNull();
    }

    @Test
    void failsWithoutSavingWhenTicksCsvIsMissing() throws IOException {
        Files.deleteIfExists(testCsv);

        assertThatThrownBy(() -> importer.importCSV(testCsv, importingUser))
            .isInstanceOf(NoSuchFileException.class);

        verify(tickRepository, never()).save(any(Tick.class));
    }

    @Test
    void failsWithoutSavingWhenNumericColumnsAreMalformed() throws IOException {
        writeTicksCsv(
            "2026-06-15,The Bulge,5.10a,Fun route,https://mountainproject.com/route/123,one,Eldorado Canyon,4,3,FLASH,REDPOINT,Trad,5.10b,80,"
        );

        assertThatThrownBy(() -> importer.importCSV(testCsv, importingUser))
            .isInstanceOf(NumberFormatException.class);

        verify(tickRepository, never()).save(any(Tick.class));
    }

    @Test
    void importsBlankLeadStyleAsUnknownRopeStyle() throws Exception {
        writeTicksCsv(
            "2026-06-15,Hi-C,V1,Fun boulder,https://mountainproject.com/route/123,1,Eldorado Canyon,4,3,Send,,Boulder,,,"
        );

        importer.importCSV(testCsv, importingUser);

        ArgumentCaptor<Tick> tickCaptor = ArgumentCaptor.forClass(Tick.class);
        verify(tickRepository).save(tickCaptor.capture());

        assertThat(tickCaptor.getValue().getRopeStyle()).isEqualTo(RopeStyle.UNKNOWN);
    }

    @Test
    void importsFellHungRowsAsAttempts() throws Exception {
        writeTicksCsv(
            "2026-06-15,The Bulge,5.10a,Worked moves,https://mountainproject.com/route/123,1,Eldorado Canyon,4,3,Lead,Fell/Hung,Trad,5.10b,80,"
        );

        Importer.ImportResult result = importer.importCSV(testCsv, importingUser);

        ArgumentCaptor<Tick> tickCaptor = ArgumentCaptor.forClass(Tick.class);
        verify(tickRepository).save(tickCaptor.capture());

        assertThat(result.importedRows()).isEqualTo(1);
        assertThat(result.skippedRows()).isZero();
        assertThat(tickCaptor.getValue().getTickType()).isEqualTo(TickType.ATTEMPT);
    }

    @Test
    void importsTopRopeRowsAsCleanTrTicks() throws Exception {
        writeTicksCsv(
            "2026-06-15,The Bulge,5.10a,TR lap,https://mountainproject.com/route/123,1,Eldorado Canyon,4,3,TR,,Trad,5.10b,80,"
        );

        Importer.ImportResult result = importer.importCSV(testCsv, importingUser);

        ArgumentCaptor<Tick> tickCaptor = ArgumentCaptor.forClass(Tick.class);
        verify(tickRepository).save(tickCaptor.capture());

        assertThat(result.importedRows()).isEqualTo(1);
        assertThat(result.skippedRows()).isZero();
        assertThat(tickCaptor.getValue().getTickType()).isEqualTo(TickType.CLEAN_TR);
    }

    @Test
    void failsWithoutSavingWhenDateFormatIsUnsupported() throws IOException {
        writeTicksCsv(
            "not-a-date,The Bulge,5.10a,Fun route,https://mountainproject.com/route/123,1,Eldorado Canyon,4,3,FLASH,REDPOINT,Trad,5.10b,80,"
        );

        assertThatThrownBy(() -> importer.importCSV(testCsv, importingUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported date format");

        verify(tickRepository, never()).save(any(Tick.class));
    }
    // main test method to run the importer against the actual inputs/ticks.csv file for inspection
    @Test
    @EnabledIfSystemProperty(named = "importer.inspectInputs", matches = "true")
    void fullCSVTest() throws Exception {
        List<Tick> savedTicks = new ArrayList<>();
        when(tickRepository.save(any(Tick.class))).thenAnswer(invocation -> {
            Tick tick = invocation.getArgument(0);
            savedTicks.add(tick);
            printImportedTick(tick);
            return tick;
        });

        try {
            importer.importCSV(importingUser);
        } catch (Exception error) {
            System.out.printf(
                "Importer failed while reading inputs/ticks.csv after %d saved tick(s): %s: %s%n",
                savedTicks.size(),
                error.getClass().getSimpleName(),
                error.getMessage()
            );
            throw error;
        }

        assertThat(savedTicks).isNotEmpty();
    }

    private void writeTicksCsv(String... rows) throws IOException {
        Files.writeString(testCsv, HEADER + System.lineSeparator()
            + String.join(System.lineSeparator(), rows)
            + System.lineSeparator());
    }

    private void printImportedTick(Tick tick) {
        System.out.printf(
            "Imported tick: climbName=%s, type=%s, date=%s, grade=%s, discipline=%s, ropeStyle=%s, pitches=%s, stars=%s, userStars=%s, height=%s, url=%s%n",
            tick.getClimbName(),
            tick.getTickType(),
            tick.getTickDate(),
            tick.getGrade(),
            tick.getDiscipline(),
            tick.getRopeStyle(),
            tick.getPitches(),
            tick.getStars(),
            tick.getUserStars(),
            tick.getClimbHeight(),
            tick.getSourceUrl()
        );
    }
}
