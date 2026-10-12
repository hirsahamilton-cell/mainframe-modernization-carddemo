package com.aws.carddemo.batch.statement;

import com.aws.carddemo.batch.statement.io.EditedAmount;
import com.aws.carddemo.batch.statement.model.Account;
import com.aws.carddemo.batch.statement.model.CardCrossReference;
import com.aws.carddemo.batch.statement.model.Customer;
import com.aws.carddemo.batch.statement.model.Statement;
import com.aws.carddemo.batch.statement.model.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The CREASTMT job: the SORT step (transactions by card number and transaction id) followed by CBSTM03A, which
 * writes one text and one HTML statement for every card in the cross-reference, in card-number order.
 */
public final class StatementJob {

    private final List<CardCrossReference> cards;
    private final Map<Long, Customer> customersById;
    private final Map<Long, Account> accountsById;

    public StatementJob(List<CardCrossReference> cards, List<Customer> customers, List<Account> accounts) {
        this.cards = cards.stream().sorted(Comparator.comparing(CardCrossReference::cardNumber)).toList();
        this.customersById = index(customers, Customer::id);
        this.accountsById = index(accounts, Account::id);
    }

    /** The text statements (80-byte lines) and HTML statements (100-byte lines) for every card. */
    public record Output(List<String> statementLines, List<String> htmlLines) {
    }

    public Output run(List<Transaction> transactions) {
        List<String> text = new ArrayList<>();
        List<String> html = new ArrayList<>();
        for (Statement statement : statements(transactions)) {
            text.addAll(StatementText.lines(statement));
            html.addAll(StatementHtml.lines(statement));
        }
        return new Output(text, html);
    }

    /**
     * Builds the statements. Transactions on cards that are not in the cross-reference are not printed; a card
     * without transactions still gets a statement with a zero total.
     */
    public List<Statement> statements(List<Transaction> transactions) {
        Map<String, List<Transaction>> byCard = transactions.stream()
                .sorted(Comparator.comparing(Transaction::cardNumber).thenComparing(Transaction::id))
                .collect(Collectors.groupingBy(Transaction::cardNumber));
        List<Statement> statements = new ArrayList<>();
        for (CardCrossReference card : cards) {
            Customer customer = lookup(customersById, card.customerId(), "CUSTFILE");
            Account account = lookup(accountsById, card.accountId(), "ACCTFILE");
            List<Transaction> cardTransactions = byCard.getOrDefault(card.cardNumber(), List.of());
            BigDecimal total = cardTransactions.stream().map(Transaction::amount)
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            requireFits(account.currentBalance(), "CURRENT BALANCE OF ACCOUNT " + account.id());
            requireFits(total, "TRANSACTION TOTAL OF CARD " + card.cardNumber());
            statements.add(new Statement(card, customer, account, cardTransactions, total));
        }
        return statements;
    }

    private static void requireFits(BigDecimal value, String what) {
        if (!EditedAmount.fits(value)) {
            throw new StatementException(what + " DOES NOT FIT ON THE STATEMENT: " + value.toPlainString());
        }
    }

    /** A random read that does not find the key returns file status 23, on which CBSTM03A abends. */
    private static <T> T lookup(Map<Long, T> index, long key, String ddName) {
        T value = index.get(key);
        if (value == null) {
            throw new StatementException("ERROR READING " + ddName, "RETURN CODE: 23");
        }
        return value;
    }

    private static <T> Map<Long, T> index(List<T> records, Function<T, Long> key) {
        return records.stream().collect(Collectors.toMap(key, Function.identity(), (first, second) -> first));
    }
}
