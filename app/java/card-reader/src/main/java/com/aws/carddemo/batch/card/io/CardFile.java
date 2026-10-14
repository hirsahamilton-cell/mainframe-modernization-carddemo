package com.aws.carddemo.batch.card.io;

import com.aws.carddemo.batch.card.model.Card;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CardFile {
    private CardFile() {
    }

    public static List<Card> load(Path path) throws CardFileException {
        BufferedReader reader;
        try {
            reader = Files.newBufferedReader(path, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw openingFailure(e);
        }

        List<Card> cards = new ArrayList<>();
        Set<String> cardNumbers = new HashSet<>();
        int lineNumber = 0;
        try (reader) {
            String line;
            while (true) {
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    throw readingFailure(lineNumber + 1, "could not read record", e);
                }
                if (line == null) {
                    break;
                }
                lineNumber++;
                line = line.replace("\r", "");
                if (line.isBlank()) {
                    continue;
                }
                Card card;
                try {
                    card = CardRecords.parse(line);
                } catch (IllegalArgumentException e) {
                    throw invalidRecord(lineNumber, e.getMessage(), e);
                }
                if (!cardNumbers.add(card.cardNumber())) {
                    throw invalidRecord(lineNumber, "duplicate card number " + card.cardNumber(), null);
                }
                cards.add(card);
            }
        } catch (CardFileException e) {
            throw e;
        } catch (IOException e) {
            throw readingFailure(lineNumber + 1, "could not read record", e);
        }
        cards.sort((first, second) -> first.cardNumber().compareTo(second.cardNumber()));
        return List.copyOf(cards);
    }

    private static CardFileException openingFailure(IOException cause) {
        return new CardFileException(List.of(
                "ERROR OPENING CARDFILE",
                "FILE STATUS IS: NNNN0035",
                "ABENDING PROGRAM"), cause);
    }

    private static CardFileException readingFailure(int lineNumber, String reason, Throwable cause) {
        return invalidRecord(lineNumber, reason, cause);
    }

    private static CardFileException invalidRecord(int lineNumber, String reason, Throwable cause) {
        return new CardFileException(List.of(
                "ERROR READING CARDFILE",
                "INVALID CARD RECORD AT LINE " + lineNumber + ": " + reason,
                "ABENDING PROGRAM"), cause);
    }
}
