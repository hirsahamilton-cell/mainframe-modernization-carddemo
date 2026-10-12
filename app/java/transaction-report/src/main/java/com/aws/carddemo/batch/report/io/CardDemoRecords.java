package com.aws.carddemo.batch.report.io;

import com.aws.carddemo.batch.report.model.CardCrossReference;
import com.aws.carddemo.batch.report.model.DateRange;
import com.aws.carddemo.batch.report.model.Transaction;
import com.aws.carddemo.batch.report.model.TransactionCategory;
import com.aws.carddemo.batch.report.model.TransactionType;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Fixed-width parsers for the CardDemo ASCII record layouts read by TRANREPT. */
public final class CardDemoRecords {

    private CardDemoRecords() {
    }

    public static <T> List<T> readAll(Path file, Function<String, T> parser) throws IOException {
        return Files.readAllLines(file, StandardCharsets.US_ASCII).stream()
                .map(line -> line.replace("\r", ""))
                .filter(line -> !line.isBlank())
                .map(parser)
                .toList();
    }

    /** Writes already-formatted lines in place, so links and permissions on an existing output are kept. */
    public static void write(Path file, List<String> lines) throws IOException {
        Files.write(file, lines, StandardCharsets.US_ASCII);
    }

    /** CVTRA05Y. */
    public static Transaction parseTransaction(String line) {
        Fields f = new Fields(line, 350);
        String id = f.text(16);
        String typeCode = f.text(2);
        int categoryCode = (int) f.number(4);
        String source = f.trimmed(10);
        f.skip(100);
        BigDecimal amount = f.decimal(11, 2);
        f.skip(9 + 50 + 50 + 10);
        String cardNumber = f.text(16);
        f.skip(26);
        return new Transaction(id, typeCode, categoryCode, source, amount, cardNumber, f.trimmed(26));
    }

    /** CVACT03Y. */
    public static CardCrossReference parseCrossReference(String line) {
        Fields f = new Fields(line, 50);
        return new CardCrossReference(f.text(16), f.number(9), f.number(11));
    }

    /** CVTRA03Y. */
    public static TransactionType parseTransactionType(String line) {
        Fields f = new Fields(line, 60);
        return new TransactionType(f.text(2), f.trimmed(50));
    }

    /** CVTRA04Y. */
    public static TransactionCategory parseTransactionCategory(String line) {
        Fields f = new Fields(line, 60);
        return new TransactionCategory(f.text(2), (int) f.number(4), f.trimmed(50));
    }

    /**
     * DATEPARM (RECLN 80): start date, one separator character, end date. An empty file means there is nothing
     * to report, as in CBTRN03C.
     */
    public static Optional<DateRange> readDateRange(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.US_ASCII).stream()
                .map(line -> line.replace("\r", ""))
                .filter(line -> !line.isBlank())
                .findFirst()
                .map(line -> {
                    Fields f = new Fields(line, 80);
                    String start = f.text(10);
                    f.skip(1);
                    return new DateRange(start, f.text(10));
                });
    }

    /** Left-justify and space-fill or truncate, like a COBOL MOVE to PIC X(n). */
    public static String pad(String value, int width) {
        return value.length() >= width ? value.substring(0, width) : value + " ".repeat(width - value.length());
    }

    private static final class Fields {
        private final String line;
        private int pos;

        Fields(String line, int recordLength) {
            this.line = pad(line, recordLength);
        }

        String text(int width) {
            String s = line.substring(pos, pos + width);
            pos += width;
            return s;
        }

        void skip(int width) {
            pos += width;
        }

        String trimmed(int width) {
            return text(width).stripTrailing();
        }

        long number(int width) {
            return Long.parseLong(text(width));
        }

        BigDecimal decimal(int width, int scale) {
            return ZonedDecimal.decode(text(width), scale);
        }
    }
}
