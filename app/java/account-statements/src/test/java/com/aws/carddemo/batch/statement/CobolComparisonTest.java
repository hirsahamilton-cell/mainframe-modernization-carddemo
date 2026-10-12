package com.aws.carddemo.batch.statement;

import com.aws.carddemo.batch.statement.model.Customer;
import com.aws.carddemo.batch.statement.model.Statement;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compares the Java statements with the statements the original CREASTMT job (SORT + CBSTM03A / CBSTM03B) wrote
 * under GnuCOBOL. Three CBSTM03A defects are fixed on purpose and the comparison accounts for exactly those:
 * <ol>
 *   <li>names and the city line are built with STRING ... DELIMITED BY ' ', so each part stops at its first
 *       space ("New Aricchester" prints as "New") and a blank middle name leaves two spaces, at which the HTML
 *       copy (DELIMITED BY '  ') then cuts off the last name;</li>
 *   <li>customer and transaction text is copied into the HTML unescaped;</li>
 *   <li>the in-memory transaction table holds 10 transactions per card: further ones overwrite the next card's
 *       entry, and the statement prints that memory instead (the "edge" scenario has a card with 12).</li>
 * </ol>
 */
class CobolComparisonTest {

    private static final int COBOL_TRANSACTIONS_PER_CARD = 10;
    private static final int TEXT_HEADER_LINES = 16;

    @ParameterizedTest
    @ValueSource(strings = {"sample", "edge"})
    void textMatchesCobolApartFromTheFixes(String scenario) throws IOException {
        SampleData.Scenario s = SampleData.Scenario.load(scenario);
        List<Statement> statements = s.job().statements(s.transactions());
        Map<String, String> legacy = legacyLines(s.customers());
        List<List<String>> cobol = split(baseline(scenario, "creastmt-statement.txt"), "START OF STATEMENT");
        assertEquals(statements.size(), cobol.size());

        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            List<String> java = stripped(StatementText.lines(statement), Function.identity(), legacy);
            List<String> expected = cobol.get(i);
            assertEquals(java.size(), expected.size(), statement.card().cardNumber());
            int n = statement.transactions().size();
            int total = TEXT_HEADER_LINES + n + 1;
            for (int line = 0; line < java.size(); line++) {
                boolean overflow = line >= TEXT_HEADER_LINES + COBOL_TRANSACTIONS_PER_CARD
                        && line < TEXT_HEADER_LINES + n || line == total && n > COBOL_TRANSACTIONS_PER_CARD;
                if (!overflow) {
                    assertEquals(java.get(line), expected.get(line), statement.card().cardNumber() + " line " + line);
                } else if (line < total) {
                    assertNotEquals(java.get(line).substring(0, 16), expected.get(line).substring(0, 16));
                }
            }
            if (n > COBOL_TRANSACTIONS_PER_CARD) {
                BigDecimal firstTen = statement.transactions().subList(0, COBOL_TRANSACTIONS_PER_CARD).stream()
                        .map(t -> t.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
                assertEquals(firstTen, amount(expected.get(total)));
                assertEquals(statement.total(), amount(java.get(total)));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"sample", "edge"})
    void htmlMatchesCobolApartFromTheFixes(String scenario) throws IOException {
        SampleData.Scenario s = SampleData.Scenario.load(scenario);
        List<Statement> statements = s.job().statements(s.transactions());
        Map<String, String> legacy = legacyLines(s.customers());
        List<List<String>> cobol = split(baseline(scenario, "creastmt-html.txt"), "<!DOCTYPE html>");
        assertEquals(statements.size(), cobol.size());
        int header = StatementHtml.lines(new Statement(statements.get(0).card(), statements.get(0).customer(),
                statements.get(0).account(), List.of(), BigDecimal.ZERO)).size() - 8;

        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            List<String> java = stripped(StatementHtml.lines(statement), CobolComparisonTest::unescape, legacy);
            List<String> expected = cobol.get(i);
            assertEquals(java.size(), expected.size(), statement.card().cardNumber());
            int overflowStart = header + 11 * COBOL_TRANSACTIONS_PER_CARD;
            int overflowEnd = header + 11 * statement.transactions().size();
            for (int line = 0; line < java.size(); line++) {
                if (line < overflowStart || line >= overflowEnd) {
                    assertEquals(java.get(line), expected.get(line), statement.card().cardNumber() + " line " + line);
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"sample", "edge"})
    void fixesChangeTheOutput(String scenario) throws IOException {
        SampleData.Scenario s = SampleData.Scenario.load(scenario);
        StatementJob.Output java = s.job().run(s.transactions());
        assertNotEquals(baseline(scenario, "creastmt-statement.txt"), stripped(java.statementLines()));
        assertTrue(java.htmlLines().stream().allMatch(line -> line.length() == StatementHtml.WIDTH));
        assertTrue(java.statementLines().stream().allMatch(line -> line.length() == StatementText.WIDTH));
        assertEquals(List.of("RETURN-CODE 0"), baseline(scenario, "creastmt-sysout.txt"));
    }

    /** Maps each fixed Java name and city line (text and HTML) to the line CBSTM03A writes for it. */
    private static Map<String, String> legacyLines(List<Customer> customers) {
        Map<String, String> legacy = new HashMap<>();
        for (Customer c : customers) {
            String name = cobolString(c.firstName(), c.middleName(), c.lastName());
            String city = cobolString(c.addressLine3(), c.stateCode(), c.countryCode(), c.zip());
            legacy.put(StatementText.name(c), name.stripTrailing());
            legacy.put(StatementText.addressLine3(c), city.stripTrailing());
            legacy.put("<p style=\"font-size:16px\">" + StatementText.name(c) + "  </p>",
                    "<p style=\"font-size:16px\">" + untilDoubleSpace(padded(name, 50)) + "  </p>");
            legacy.put("<p>" + StatementText.addressLine3(c) + "  </p>", "<p>" + untilDoubleSpace(padded(city, 80)) + "  </p>");
        }
        return legacy;
    }

    /** STRING part DELIMITED BY ' ' ' ' DELIMITED BY SIZE ... */
    private static String cobolString(String... parts) {
        return Stream.of(parts).map(p -> p.contains(" ") ? p.substring(0, p.indexOf(' ')) : p)
                .map(p -> p + " ").collect(Collectors.joining());
    }

    private static String untilDoubleSpace(String value) {
        int i = value.indexOf("  ");
        return i < 0 ? value : value.substring(0, i);
    }

    /** MOVE to PIC X(width). */
    private static String padded(String value, int width) {
        return String.format("%-" + width + "." + width + "s", value);
    }

    private static String unescape(String line) {
        return line.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }

    private static List<String> stripped(List<String> lines, Function<String, String> undoEscaping,
                                         Map<String, String> legacy) {
        return lines.stream().map(String::stripTrailing).map(undoEscaping)
                .map(line -> legacy.getOrDefault(line, line)).toList();
    }

    private static List<String> stripped(List<String> lines) {
        return lines.stream().map(String::stripTrailing).toList();
    }

    private static List<String> baseline(String scenario, String file) throws IOException {
        return Files.readAllLines(SampleData.cobolBaseline(scenario, file), StandardCharsets.US_ASCII);
    }

    private static List<List<String>> split(List<String> lines, String firstLineMarker) {
        List<List<String>> parts = new ArrayList<>();
        for (String line : lines) {
            if (line.contains(firstLineMarker)) {
                parts.add(new ArrayList<>());
            }
            parts.get(parts.size() - 1).add(line);
        }
        return parts;
    }

    private static BigDecimal amount(String totalLine) {
        String edited = totalLine.substring(totalLine.indexOf('$') + 1).strip();
        BigDecimal value = new BigDecimal(edited.replace("-", ""));
        return edited.endsWith("-") ? value.negate() : value;
    }
}
