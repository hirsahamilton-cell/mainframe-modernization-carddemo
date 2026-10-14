package com.aws.carddemo.batch.card;

import com.aws.carddemo.batch.card.io.CardRecords;
import com.aws.carddemo.batch.card.model.Card;
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

class CardFileReaderMainTest {
    @TempDir
    Path tempDir;

    @Test
    void wrongArgumentCountPrintsUsageAndReturnsEight() {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(stderr, true, StandardCharsets.UTF_8));
        try {
            assertEquals(8, CardFileReaderMain.run(new String[0], ignored -> { }));
        } finally {
            System.setErr(originalErr);
        }
        assertEquals("usage: java -jar card-reader.jar CARDFILE" + System.lineSeparator(),
                stderr.toString(StandardCharsets.UTF_8));
    }

    @Test
    void badRecordReturnsTwelveWithExpectedSysoutLines() throws Exception {
        Path file = tempDir.resolve("bad-carddata.txt");
        String invalid = CardRecords.format(new Card(
                "0000000000000001", 1, 123, "CARDHOLDER", "2027-01-01", "Y", " ".repeat(59)));
        invalid = invalid.substring(0, 16) + "X" + invalid.substring(17);
        Files.writeString(file, invalid + "\n", StandardCharsets.US_ASCII);
        List<String> sysout = new ArrayList<>();

        int returnCode = CardFileReaderMain.run(new String[]{file.toString()}, sysout::add);

        assertEquals(12, returnCode);
        assertEquals(List.of(
                "START OF EXECUTION OF PROGRAM CBACT02C",
                "ERROR READING CARDFILE",
                "INVALID CARD RECORD AT LINE 1: account id is not numeric",
                "ABENDING PROGRAM"), sysout);
    }
}
