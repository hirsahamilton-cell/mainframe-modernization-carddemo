package com.aws.carddemo.batch.card;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobolComparisonTest {
    private static final Path BASELINES = Path.of("src/test/resources/cobol-baseline");
    private static final Path SCENARIOS = Path.of("src/test/resources/scenarios");

    @Test
    void matchesSampleEdgeAndEmptyBaselines() throws Exception {
        compareSuccessfulScenario("sample", Path.of(System.getProperty("carddemo.data.dir"), "carddata.txt"));
        compareSuccessfulScenario("edge", SCENARIOS.resolve("edge.txt"));
        compareSuccessfulScenario("empty", SCENARIOS.resolve("empty.txt"));
    }

    @Test
    void matchesMissingFileErrorBeforeCobolCee3abdCall() throws Exception {
        List<String> baseline = Files.readAllLines(BASELINES.resolve("missing/readcard-sysout.txt"));
        assertEquals("RETURN-CODE 1", baseline.get(baseline.size() - 1));
        int abendIndex = baseline.indexOf("ABENDING PROGRAM");
        assertTrue(abendIndex >= 0);

        List<String> actual = new ArrayList<>();
        int returnCode = CardFileReaderMain.run(
                new String[]{SCENARIOS.resolve("missing.txt").toString()}, actual::add);

        // GnuCOBOL cannot resolve the IBM CEE3ABD routine after these DISPLAY lines.
        assertEquals(baseline.subList(0, abendIndex + 1), actual);
        assertEquals(12, returnCode);
    }

    private static void compareSuccessfulScenario(String scenario, Path input) throws Exception {
        List<String> baseline = Files.readAllLines(BASELINES.resolve(scenario + "/readcard-sysout.txt"));
        assertEquals("RETURN-CODE 0", baseline.get(baseline.size() - 1));
        List<String> expected = baseline.subList(0, baseline.size() - 1);
        List<String> actual = new ArrayList<>();

        int returnCode = CardFileReaderMain.run(new String[]{input.toString()}, actual::add);

        assertEquals(0, returnCode);
        assertEquals(expected, actual);
    }
}
