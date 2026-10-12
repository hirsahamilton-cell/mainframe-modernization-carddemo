package com.aws.carddemo.batch.statement;

import com.aws.carddemo.batch.statement.model.Statement;
import com.aws.carddemo.batch.statement.model.Transaction;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.aws.carddemo.batch.statement.StatementTestData.account;
import static com.aws.carddemo.batch.statement.StatementTestData.card;
import static com.aws.carddemo.batch.statement.StatementTestData.customer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementFormatTest {

    private static Statement statement(String middleName, String description) {
        Transaction t = new Transaction("0000000000000001", description, new BigDecimal("-12.30"), "4111");
        return new Statement(card("4111", 1, 7), customer(1, "Ana Maria", middleName, "de la Cruz", "Port Angeles"),
                account(7, "-50.00"), List.of(t), t.amount());
    }

    @Test
    void textUsesTheWholeNameAndCityAndSkipsBlankParts() {
        List<String> lines = StatementText.lines(statement("", "Coffee"));
        assertEquals("Ana Maria de la Cruz", lines.get(1).stripTrailing());
        assertEquals("1 Main St", lines.get(2).stripTrailing());
        assertEquals("", lines.get(3).stripTrailing());
        assertEquals("Port Angeles WA USA 98101", lines.get(4).stripTrailing());
        assertEquals("Account ID         :00000000007", lines.get(8).stripTrailing());
        assertEquals("Current Balance    :000000050.00-", lines.get(9).stripTrailing());
        assertEquals("FICO Score         :700", lines.get(10).stripTrailing());
        assertEquals("0000000000000001 " + "Coffee" + " ".repeat(43) + "$       12.30-", lines.get(16));
        assertEquals("Total EXP:" + " ".repeat(56) + "$       12.30-", lines.get(18));
        lines.forEach(line -> assertEquals(StatementText.WIDTH, line.length()));
    }

    @Test
    void textCutsLongDescriptionsAt49Characters() {
        String line = StatementText.lines(statement("", "x".repeat(100))).get(16);
        assertEquals("0000000000000001 " + "x".repeat(49) + "$       12.30-", line);
    }

    @Test
    void htmlEscapesTextAndKeepsLinesAt100Bytes() {
        List<String> lines = StatementHtml.lines(statement("<b>", "Smith & Sons <Hardware>"));
        assertTrue(lines.contains(pad("<p style=\"font-size:16px\">Ana Maria &lt;b&gt; de la Cruz  </p>")));
        assertTrue(lines.contains(pad("<p>Smith &amp; Sons &lt;Hardware&gt;" + " ".repeat(26) + "</p>")));
        assertTrue(lines.contains(pad("<p>Current Balance    : 000000050.00-</p>")));
        assertTrue(lines.contains(pad("<p>       12.30-</p>")));
        lines.forEach(line -> assertEquals(StatementHtml.WIDTH, line.length()));
    }

    @Test
    void htmlDropsCharactersThatNoLongerFitAfterEscaping() {
        String line = StatementHtml.paragraph("<p>", "&".repeat(49), "</p>");
        assertEquals("<p>" + "&amp;".repeat(18) + "</p>", line);
    }

    @Test
    void htmlDocumentPerStatement() {
        List<String> lines = StatementHtml.lines(statement("", "Coffee"));
        assertEquals("<!DOCTYPE html>", lines.get(0).stripTrailing());
        assertEquals("<h3>Statement for Account Number: 00000000007         </h3>", lines.get(10).stripTrailing());
        assertEquals("</html>", lines.get(lines.size() - 1).stripTrailing());
    }

    private static String pad(String line) {
        return String.format("%-100s", line);
    }
}
