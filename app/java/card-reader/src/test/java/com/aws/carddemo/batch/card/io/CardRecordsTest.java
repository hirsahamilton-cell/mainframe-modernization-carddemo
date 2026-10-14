package com.aws.carddemo.batch.card.io;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardRecordsTest {
    @Test
    void shippedRecordRoundTripsByteForByte() throws Exception {
        Path sample = Path.of(System.getProperty("carddemo.data.dir"), "carddata.txt");
        String line = Files.readAllLines(sample, StandardCharsets.US_ASCII).get(0);

        assertEquals(line, CardRecords.format(CardRecords.parse(line)));
    }

    @Test
    void shortRecordIsPaddedToTheCobolRecordLength() throws Exception {
        Path sample = Path.of(System.getProperty("carddemo.data.dir"), "carddata.txt");
        String line = Files.readAllLines(sample, StandardCharsets.US_ASCII).get(0).substring(0, 91);

        assertEquals(line + " ".repeat(59), CardRecords.format(CardRecords.parse(line)));
    }

    @Test
    void rejectsRecordLongerThan150Characters() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> CardRecords.parse("x".repeat(151)));

        assertEquals("record is longer than 150 characters", error.getMessage());
    }

    @Test
    void rejectsNonNumericAccountIdAndCvv() throws Exception {
        Path sample = Path.of(System.getProperty("carddemo.data.dir"), "carddata.txt");
        String line = Files.readAllLines(sample, StandardCharsets.US_ASCII).get(0);
        String badAccount = line.substring(0, 16) + "X" + line.substring(17);
        String badCvv = line.substring(0, 27) + "X" + line.substring(28);

        assertEquals("account id is not numeric",
                assertThrows(IllegalArgumentException.class, () -> CardRecords.parse(badAccount)).getMessage());
        assertEquals("CVV is not numeric",
                assertThrows(IllegalArgumentException.class, () -> CardRecords.parse(badCvv)).getMessage());
    }
}
