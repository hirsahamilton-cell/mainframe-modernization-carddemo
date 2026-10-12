package com.aws.carddemo.batch.statement;

import com.aws.carddemo.batch.statement.model.Account;
import com.aws.carddemo.batch.statement.model.CardCrossReference;
import com.aws.carddemo.batch.statement.model.Customer;
import com.aws.carddemo.batch.statement.model.Transaction;

import java.math.BigDecimal;

final class StatementTestData {

    private StatementTestData() {
    }

    static Customer customer(long id, String first, String middle, String last, String city) {
        return new Customer(id, first, middle, last, "1 Main St", "", city, "WA", "USA", "98101", "700");
    }

    static Account account(long id, String balance) {
        return new Account(id, new BigDecimal(balance));
    }

    static CardCrossReference card(String number, long customerId, long accountId) {
        return new CardCrossReference(number, customerId, accountId);
    }

    static Transaction transaction(String card, String id, String amount) {
        return new Transaction(id, "Purchase " + id, new BigDecimal(amount), card);
    }
}
