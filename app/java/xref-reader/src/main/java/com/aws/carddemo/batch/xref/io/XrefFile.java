package com.aws.carddemo.batch.xref.io;

import com.aws.carddemo.batch.xref.model.CardCrossReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class XrefFile {
    private XrefFile() {
    }

    public static List<CardCrossReference> load(Path path) throws XrefFileException {
        BufferedReader reader;
        try {
            reader = Files.newBufferedReader(path, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new XrefFileException(List.of(
                    "ERROR OPENING XREFFILE",
                    "FILE STATUS IS: NNNN0035",
                    "ABENDING PROGRAM"), e);
        }

        List<CardCrossReference> xrefs = new ArrayList<>();
        Set<String> cardNumbers = new HashSet<>();
        int lineNumber = 0;
        try (reader) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.replace("\r", "");
                if (line.isBlank()) {
                    continue;
                }
                CardCrossReference xref;
                try {
                    xref = XrefRecords.parse(line);
                } catch (IllegalArgumentException e) {
                    throw invalidRecord(lineNumber, e.getMessage(), e);
                }
                if (!cardNumbers.add(xref.cardNumber())) {
                    throw invalidRecord(lineNumber, "duplicate card number " + xref.cardNumber(), null);
                }
                xrefs.add(xref);
            }
        } catch (IOException e) {
            throw invalidRecord(lineNumber + 1, "could not read record", e);
        }
        xrefs.sort(Comparator.comparing(CardCrossReference::cardNumber));
        return List.copyOf(xrefs);
    }

    private static XrefFileException invalidRecord(int lineNumber, String reason, Throwable cause) {
        return new XrefFileException(List.of(
                "ERROR READING XREFFILE",
                "INVALID XREF RECORD AT LINE " + lineNumber + ": " + reason,
                "ABENDING PROGRAM"), cause);
    }
}
