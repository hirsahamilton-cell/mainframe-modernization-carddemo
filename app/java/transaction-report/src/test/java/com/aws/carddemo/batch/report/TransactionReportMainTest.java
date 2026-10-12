package com.aws.carddemo.batch.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionReportMainTest {

    @TempDir
    Path dir;

    @Test
    void writesThe133ByteReport() throws IOException {
        Path report = dir.resolve("tranrept.txt");
        List<String> sysout = new ArrayList<>();
        assertEquals(0, TransactionReportMain.run(args(SampleData.Scenario.load("edge").transactFile(),
                dateParm("2026-01-01 2026-01-31"), report), sysout::add));
        List<String> lines = Files.readAllLines(report, StandardCharsets.US_ASCII);
        assertTrue(lines.size() > 50);
        lines.forEach(line -> assertEquals(133, line.length()));
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBTRN03C", "Reporting from 2026-01-01 to 2026-01-31",
                "END OF EXECUTION OF PROGRAM CBTRN03C"), sysout);
    }

    @Test
    void abendsWithoutWritingTheReport() throws IOException {
        Path transact = dir.resolve("transact.txt");
        String line = Files.readAllLines(SampleData.Scenario.load("edge").transactFile()).get(0);
        Files.writeString(transact, line.substring(0, 16) + "09" + line.substring(18) + "\n");
        Path report = Files.writeString(dir.resolve("tranrept.txt"), "previous report\n");
        List<String> sysout = new ArrayList<>();
        assertEquals(12, TransactionReportMain.run(args(transact, dateParm("2026-01-01 2026-01-31"), report),
                sysout::add));
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBTRN03C", "Reporting from 2026-01-01 to 2026-01-31",
                "INVALID TRANSACTION TYPE : 09", "ABENDING PROGRAM"), sysout);
        assertEquals("previous report\n", Files.readString(report));
    }

    @Test
    void emptyInputGivesAnEmptyReport() throws IOException {
        Path report = dir.resolve("tranrept.txt");
        Path transact = Files.writeString(dir.resolve("transact.txt"), "");
        assertEquals(0, TransactionReportMain.run(args(transact, dateParm("2026-01-01 2026-01-31"), report),
                line -> { }));
        assertEquals("", Files.readString(report));
    }

    private Path dateParm(String content) throws IOException {
        return Files.writeString(dir.resolve("dateparm.txt"), String.format("%-80s%n", content));
    }

    private static String[] args(Path transact, Path dateParm, Path report) {
        return new String[] {transact.toString(), SampleData.ascii("cardxref.txt").toString(),
                SampleData.ascii("trantype.txt").toString(), SampleData.ascii("trancatg.txt").toString(),
                dateParm.toString(), report.toString()};
    }
}
