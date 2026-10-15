package com.aws.carddemo.batch.xref.io;

import com.aws.carddemo.batch.xref.model.CardCrossReference;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XrefRecordsTest {
    private static String firstSampleLine() throws Exception {
        Path sample = Path.of(System.getProperty("carddemo.data.dir"), "cardxref.txt");
        return Files.readAllLines(sample, StandardCharsets.US_ASCII).get(0);
    }

    @Test
    void parsesShippedRecordFields() throws Exception {
        CardCrossReference xref = XrefRecords.parse(firstSampleLine());

        assertEquals(new CardCrossReference("0500024453765740", 50, 50, " ".repeat(14)), xref);
    }

    @Test
    void shippedRecordIsPaddedToTheCobolRecordLength() throws Exception {
        String line = firstSampleLine();

        assertEquals(line + " ".repeat(14), XrefRecords.format(XrefRecords.parse(line)));
    }

    @Test
    void fullWidthRecordWithFillerRoundTripsByteForByte() {
        String line = "4000000000000099999999999999999999990123456789ABCD";

        assertEquals(line, XrefRecords.format(XrefRecords.parse(line)));
    }

    @Test
    void rejectsRecordLongerThan50Characters() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> XrefRecords.parse("x".repeat(51)));

        assertEquals("record is longer than 50 characters", error.getMessage());
    }

    @Test
    void rejectsNonNumericOrMissingIds() throws Exception {
        String line = firstSampleLine();
        String badCustomer = line.substring(0, 16) + "X" + line.substring(17);
        String badAccount = line.substring(0, 35) + "X";
        String truncated = line.substring(0, 30);

        assertEquals("customer id is not numeric",
                assertThrows(IllegalArgumentException.class, () -> XrefRecords.parse(badCustomer)).getMessage());
        assertEquals("account id is not numeric",
                assertThrows(IllegalArgumentException.class, () -> XrefRecords.parse(badAccount)).getMessage());
        assertEquals("account id is not numeric",
                assertThrows(IllegalArgumentException.class, () -> XrefRecords.parse(truncated)).getMessage());
    }
}
