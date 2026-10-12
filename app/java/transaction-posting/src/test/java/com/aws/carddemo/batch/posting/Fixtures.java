package com.aws.carddemo.batch.posting;

import com.aws.carddemo.batch.posting.model.Account;
import com.aws.carddemo.batch.posting.model.CardCrossReference;
import com.aws.carddemo.batch.posting.model.Transaction;

import java.math.BigDecimal;

final class Fixtures {

    static final String CARD = "4111111111111111";
    static final long ACCOUNT_ID = 1;

    private Fixtures() {
    }

    static Account account(String creditLimit, String cycleCredit, String cycleDebit, String expirationDate) {
        return new Account(ACCOUNT_ID, "Y", new BigDecimal("100.00"), new BigDecimal(creditLimit),
                new BigDecimal("500.00"), "2020-01-01", expirationDate, expirationDate,
                new BigDecimal(cycleCredit), new BigDecimal(cycleDebit), "12345", "");
    }

    static CardCrossReference xref() {
        return new CardCrossReference(CARD, 1, ACCOUNT_ID);
    }

    static Transaction transaction(String id, String amount, String originDate) {
        return transaction(id, CARD, "01", 1, amount, originDate);
    }

    static Transaction transaction(String id, String card, String type, int category, String amount, String originDate) {
        return new Transaction(id, type, category, "POS TERM", "Purchase", new BigDecimal(amount), 800000000,
                "Merchant", "City", "12345", card, originDate + " 12:00:00.000000", "");
    }
}
