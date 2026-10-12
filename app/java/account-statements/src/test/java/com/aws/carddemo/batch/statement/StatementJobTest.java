package com.aws.carddemo.batch.statement;

import com.aws.carddemo.batch.statement.model.CardCrossReference;
import com.aws.carddemo.batch.statement.model.Statement;
import com.aws.carddemo.batch.statement.model.Transaction;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.aws.carddemo.batch.statement.StatementTestData.account;
import static com.aws.carddemo.batch.statement.StatementTestData.card;
import static com.aws.carddemo.batch.statement.StatementTestData.customer;
import static com.aws.carddemo.batch.statement.StatementTestData.transaction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatementJobTest {

    private final StatementJob job = new StatementJob(
            List.of(card("2222", 2, 20), card("1111", 1, 10), card("3333", 1, 10)),
            List.of(customer(1, "Ann", "", "Lee", "Sea"), customer(2, "Bo", "", "Ng", "Tac")),
            List.of(account(10, "100.00"), account(20, "-5.00")));

    @Test
    void oneStatementPerCardInCardOrderWithTransactionsInIdOrder() {
        List<Statement> statements = job.statements(List.of(
                transaction("2222", "0000000000000009", "1.00"),
                transaction("1111", "0000000000000005", "-2.50"),
                transaction("9999", "0000000000000001", "7.00"),
                transaction("1111", "0000000000000003", "0.05")));
        assertEquals(List.of("1111", "2222", "3333"),
                statements.stream().map(s -> s.card().cardNumber()).toList());
        assertEquals(List.of("0000000000000003", "0000000000000005"),
                statements.get(0).transactions().stream().map(Transaction::id).toList());
        assertEquals(new BigDecimal("-2.45"), statements.get(0).total());
        assertEquals(new BigDecimal("1.00"), statements.get(1).total());
        assertEquals(List.of(), statements.get(2).transactions());
        assertEquals(new BigDecimal("0.00"), statements.get(2).total());
    }

    @Test
    void noTransactionsStillGivesEveryStatement() {
        StatementJob.Output output = job.run(List.of());
        assertEquals(3 * 19, output.statementLines().size());
    }

    @Test
    void noLimitOnTransactionsPerCardOrOnCards() {
        List<CardCrossReference> cards = new ArrayList<>();
        List<Transaction> transactions = new ArrayList<>();
        IntStream.rangeClosed(1, 60).forEach(c -> {
            String number = String.format("%016d", c);
            cards.add(card(number, 1, 10));
            IntStream.rangeClosed(1, 15).forEach(t -> transactions.add(
                    transaction(number, String.format("%016d", t), "1.00")));
        });
        List<Statement> statements = new StatementJob(cards, List.of(customer(1, "A", "", "B", "C")),
                List.of(account(10, "0.00"))).statements(transactions);
        assertEquals(60, statements.size());
        statements.forEach(s -> assertEquals(new BigDecimal("15.00"), s.total()));
    }

    @Test
    void missingCustomerOrAccountAbends() {
        StatementException e = assertThrows(StatementException.class, () -> new StatementJob(
                List.of(card("1111", 3, 10)), List.of(), List.of(account(10, "0"))).statements(List.of()));
        assertEquals(List.of("ERROR READING CUSTFILE", "RETURN CODE: 23"), e.sysoutLines());
        e = assertThrows(StatementException.class, () -> new StatementJob(
                List.of(card("1111", 1, 11)), List.of(customer(1, "A", "", "B", "C")), List.of())
                .statements(List.of()));
        assertEquals(List.of("ERROR READING ACCTFILE", "RETURN CODE: 23"), e.sysoutLines());
    }

    @Test
    void totalTooLargeForTheStatementAbends() {
        assertThrows(StatementException.class, () -> job.statements(List.of(
                transaction("1111", "0000000000000001", "999999999.99"),
                transaction("1111", "0000000000000002", "0.01"))));
    }
}
