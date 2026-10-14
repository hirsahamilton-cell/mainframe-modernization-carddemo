package com.aws.carddemo.batch.card.io;

import com.aws.carddemo.batch.card.model.Card;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardFileTest {
    @TempDir
    Path tempDir;

    @Test
    void sortsCardsByCardNumber() throws Exception {
        Path file = tempDir.resolve("cards.txt");
        Files.writeString(file, CardRecords.format(card("0000000000000002", 2))
                + "\n" + CardRecords.format(card("0000000000000001", 1)) + "\n", StandardCharsets.US_ASCII);

        assertEquals(List.of("0000000000000001", "0000000000000002"),
                CardFile.load(file).stream().map(Card::cardNumber).toList());
    }

    @Test
    void rejectsDuplicateCardNumbers() throws Exception {
        Path file = tempDir.resolve("duplicates.txt");
        String record = CardRecords.format(card("0000000000000001", 1));
        Files.writeString(file, record + "\n" + record + "\n", StandardCharsets.US_ASCII);

        CardFileException error = assertThrows(CardFileException.class, () -> CardFile.load(file));

        assertEquals("ERROR READING CARDFILE", error.sysoutLines().get(0));
        assertEquals("INVALID CARD RECORD AT LINE 2: duplicate card number 0000000000000001",
                error.sysoutLines().get(1));
    }

    @Test
    void skipsBlankLinesAndCarriageReturns() throws Exception {
        Path file = tempDir.resolve("blank-lines.txt");
        String record = CardRecords.format(card("0000000000000001", 1));
        String embeddedCarriageReturn = record.substring(0, 100) + "\r" + record.substring(100);
        Files.writeString(file, "\n  \n" + embeddedCarriageReturn + "\r\n", StandardCharsets.US_ASCII);

        List<Card> cards = CardFile.load(file);

        assertEquals(1, cards.size());
        assertEquals("0000000000000001", cards.get(0).cardNumber());
    }

    @Test
    void returnsAnImmutableList() throws Exception {
        Path file = tempDir.resolve("cards.txt");
        Files.writeString(file, CardRecords.format(card("0000000000000001", 1)), StandardCharsets.US_ASCII);

        List<Card> cards = CardFile.load(file);

        assertThrows(UnsupportedOperationException.class, () -> cards.add(card("0000000000000002", 2)));
    }

    private static Card card(String cardNumber, long accountId) {
        return new Card(cardNumber, accountId, 123, "CARDHOLDER", "2027-01-01", "Y", " ".repeat(59));
    }
}
