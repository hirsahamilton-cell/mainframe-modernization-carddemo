package com.aws.carddemo.batch.xref;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XrefFileReaderMainTest {
    @TempDir
    Path tempDir;

    @Test
    void wrongArgumentCountPrintsUsageAndReturnsEight() {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(stderr, true, StandardCharsets.UTF_8));
        try {
            assertEquals(8, XrefFileReaderMain.run(new String[0], ignored -> { }));
        } finally {
            System.setErr(originalErr);
        }
        assertEquals("usage: java -jar xref-reader.jar XREFFILE" + System.lineSeparator(),
                stderr.toString(StandardCharsets.UTF_8));
    }

    @Test
    void badRecordReturnsTwelveWithExpectedSysoutLines() throws Exception {
        Path file = tempDir.resolve("bad-cardxref.txt");
        Files.writeString(file, "0000000000000001X00000001000000000001\n", StandardCharsets.US_ASCII);
        List<String> sysout = new ArrayList<>();

        int returnCode = XrefFileReaderMain.run(new String[]{file.toString()}, sysout::add);

        assertEquals(12, returnCode);
        assertEquals(List.of(
                "START OF EXECUTION OF PROGRAM CBACT03C",
                "ERROR READING XREFFILE",
                "INVALID XREF RECORD AT LINE 1: customer id is not numeric",
                "ABENDING PROGRAM"), sysout);
    }
}
