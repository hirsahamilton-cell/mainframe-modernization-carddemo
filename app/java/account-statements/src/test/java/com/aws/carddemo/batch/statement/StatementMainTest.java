package com.aws.carddemo.batch.statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementMainTest {

    @TempDir
    Path dir;

    @Test
    void writesFixedWidthStatementAndHtmlFiles() throws IOException {
        Path stmt = dir.resolve("statemnt.ps");
        Path html = dir.resolve("statemnt.html");
        List<String> sysout = new ArrayList<>();
        assertEquals(0, StatementMain.run(SampleData.Scenario.load("edge").args(stmt, html), sysout::add));
        List<String> text = Files.readAllLines(stmt, StandardCharsets.US_ASCII);
        List<String> page = Files.readAllLines(html, StandardCharsets.US_ASCII);
        assertEquals(5, text.stream().filter(l -> l.contains("START OF STATEMENT")).count());
        text.forEach(line -> assertEquals(80, line.length()));
        page.forEach(line -> assertEquals(100, line.length()));
        assertEquals(List.of(), sysout);
    }

    @Test
    void emptyTransactionFileGivesStatementsWithZeroTotals() throws IOException {
        SampleData.Scenario edge = SampleData.Scenario.load("edge");
        Path transact = Files.writeString(dir.resolve("transact.txt"), "");
        SampleData.Scenario empty = new SampleData.Scenario(transact, edge.xrefFile(), edge.acctFile(),
                edge.custFile());
        Path stmt = dir.resolve("statemnt.ps");
        assertEquals(0, StatementMain.run(empty.args(stmt, dir.resolve("statemnt.html")), line -> { }));
        List<String> text = Files.readAllLines(stmt, StandardCharsets.US_ASCII);
        assertEquals(5, text.stream().filter(l -> l.startsWith("Total EXP:") && l.contains("$         .00"))
                .count());
    }

    @Test
    void abendsWithoutWritingOutputs() throws IOException {
        SampleData.Scenario edge = SampleData.Scenario.load("edge");
        Path xref = Files.writeString(dir.resolve("xref.txt"), "4111111111111111" + "000000999" + "00000000001"
                + " ".repeat(14) + "\n");
        SampleData.Scenario bad = new SampleData.Scenario(edge.transactFile(), xref, edge.acctFile(),
                edge.custFile());
        Path stmt = Files.writeString(dir.resolve("statemnt.ps"), "previous\n");
        List<String> sysout = new ArrayList<>();
        assertEquals(12, StatementMain.run(bad.args(stmt, dir.resolve("statemnt.html")), sysout::add));
        assertEquals(List.of("ERROR READING CUSTFILE", "RETURN CODE: 23", "ABENDING PROGRAM"), sysout);
        assertEquals("previous\n", Files.readString(stmt));
    }

    @Test
    void wrongArgumentCount() throws IOException {
        assertEquals(8, StatementMain.run(new String[] {"only-one"}, line -> { }));
    }
}
