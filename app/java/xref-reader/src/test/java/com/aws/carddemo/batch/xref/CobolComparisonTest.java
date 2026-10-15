package com.aws.carddemo.batch.xref;

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
    void matchesSampleEdgeAndEmptyBaselinesWithEachRecordPrintedOnce() throws Exception {
        compareSuccessfulScenario("sample", Path.of(System.getProperty("carddemo.data.dir"), "cardxref.txt"), 50);
        compareSuccessfulScenario("edge", SCENARIOS.resolve("edge.txt"), 6);
        compareSuccessfulScenario("empty", SCENARIOS.resolve("empty.txt"), 0);
    }

    @Test
    void matchesMissingFileErrorBeforeCobolCee3abdCall() throws Exception {
        List<String> baseline = Files.readAllLines(BASELINES.resolve("missing/readxref-sysout.txt"));
        assertEquals("RETURN-CODE 1", baseline.get(baseline.size() - 1));
        int abendIndex = baseline.indexOf("ABENDING PROGRAM");
        assertTrue(abendIndex >= 0);

        List<String> actual = new ArrayList<>();
        int returnCode = XrefFileReaderMain.run(
                new String[]{SCENARIOS.resolve("missing.txt").toString()}, actual::add);

        // GnuCOBOL cannot resolve the IBM CEE3ABD routine after these DISPLAY lines.
        assertEquals(baseline.subList(0, abendIndex + 1), actual);
        assertEquals(12, returnCode);
    }

    private static void compareSuccessfulScenario(String scenario, Path input, int recordCount) throws Exception {
        List<String> baseline = Files.readAllLines(BASELINES.resolve(scenario + "/readxref-sysout.txt"));
        assertEquals("RETURN-CODE 0", baseline.get(baseline.size() - 1));
        List<String> cobolSysout = baseline.subList(0, baseline.size() - 1);
        List<String> actual = new ArrayList<>();

        int returnCode = XrefFileReaderMain.run(new String[]{input.toString()}, actual::add);

        assertEquals(0, returnCode);
        assertEquals(withoutDuplicateRecordDisplays(cobolSysout), actual);
        assertEquals(recordCount + 2, actual.size());
    }

    // CBACT03C DISPLAYs each record in 1000-XREFFILE-GET-NEXT and again in the main loop.
    private static List<String> withoutDuplicateRecordDisplays(List<String> cobolSysout) {
        List<String> records = cobolSysout.subList(1, cobolSysout.size() - 1);
        assertEquals(0, records.size() % 2);
        List<String> expected = new ArrayList<>();
        expected.add(cobolSysout.get(0));
        for (int i = 0; i < records.size(); i += 2) {
            assertEquals(records.get(i), records.get(i + 1));
            expected.add(records.get(i));
        }
        expected.add(cobolSysout.get(cobolSysout.size() - 1));
        return expected;
    }
}
