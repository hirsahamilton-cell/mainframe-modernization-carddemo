package com.aws.carddemo.batch.xref.io;

import com.aws.carddemo.batch.xref.model.CardCrossReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XrefFileTest {
    @TempDir
    Path tempDir;

    @Test
    void sortsCrossReferencesByCardNumber() throws Exception {
        Path file = tempDir.resolve("xrefs.txt");
        Files.writeString(file, XrefRecords.format(xref("0000000000000002", 2))
                + "\n" + XrefRecords.format(xref("0000000000000001", 1)) + "\n", StandardCharsets.US_ASCII);

        assertEquals(List.of("0000000000000001", "0000000000000002"),
                XrefFile.load(file).stream().map(CardCrossReference::cardNumber).toList());
    }

    @Test
    void rejectsDuplicateCardNumbers() throws Exception {
        Path file = tempDir.resolve("duplicates.txt");
        String record = XrefRecords.format(xref("0000000000000001", 1));
        Files.writeString(file, record + "\n" + record + "\n", StandardCharsets.US_ASCII);

        XrefFileException error = assertThrows(XrefFileException.class, () -> XrefFile.load(file));

        assertEquals(List.of(
                "ERROR READING XREFFILE",
                "INVALID XREF RECORD AT LINE 2: duplicate card number 0000000000000001",
                "ABENDING PROGRAM"), error.sysoutLines());
    }

    @Test
    void missingFileReportsCobolOpenStatus() {
        XrefFileException error = assertThrows(
                XrefFileException.class, () -> XrefFile.load(tempDir.resolve("missing.txt")));

        assertEquals(List.of("ERROR OPENING XREFFILE", "FILE STATUS IS: NNNN0035", "ABENDING PROGRAM"),
                error.sysoutLines());
    }

    @Test
    void skipsBlankLinesAndCarriageReturns() throws Exception {
        Path file = tempDir.resolve("blank-lines.txt");
        String record = XrefRecords.format(xref("0000000000000001", 1));
        Files.writeString(file, "\n  \n" + record + "\r\n", StandardCharsets.US_ASCII);

        List<CardCrossReference> xrefs = XrefFile.load(file);

        assertEquals(List.of(xref("0000000000000001", 1)), xrefs);
    }

    private static CardCrossReference xref(String cardNumber, long accountId) {
        return new CardCrossReference(cardNumber, accountId, accountId, " ".repeat(14));
    }
}
