package com.aws.carddemo.batch.statement.io;

import com.aws.carddemo.batch.statement.model.Account;
import com.aws.carddemo.batch.statement.model.CardCrossReference;
import com.aws.carddemo.batch.statement.model.Customer;
import com.aws.carddemo.batch.statement.model.Transaction;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardDemoRecordsTest {

    private static Path ascii(String name) {
        return Path.of(System.getProperty("carddemo.data.dir", "../../data/ASCII")).resolve(name);
    }

    @Test
    void parsesTheShippedCustomers() throws IOException {
        Customer first = CardDemoRecords.readAll(ascii("custdata.txt"), CardDemoRecords::parseCustomer).get(0);
        assertEquals(1, first.id());
        assertEquals(3, first.ficoScore().length());
        assertEquals(first.firstName().strip(), first.firstName());
    }

    @Test
    void parsesTheShippedCrossReferenceAndAccounts() throws IOException {
        CardCrossReference card = CardDemoRecords.readAll(ascii("cardxref.txt"),
                CardDemoRecords::parseCrossReference).get(0);
        assertEquals(16, card.cardNumber().length());
        Account account = CardDemoRecords.readAll(ascii("acctdata.txt"), CardDemoRecords::parseAccount).get(0);
        assertEquals(1, account.id());
        assertEquals(2, account.currentBalance().scale());
    }

    @Test
    void parsesATransactionWithANegativeAmount() {
        String line = CardDemoRecords.pad("0000000000000042" + "01" + "0001" + CardDemoRecords.pad("POS TERM", 10)
                + CardDemoRecords.pad("Refund", 100) + "0000000255}" + "000000001"
                + " ".repeat(110) + "4111111111111111", 350);
        Transaction t = CardDemoRecords.parseTransaction(line);
        assertEquals("0000000000000042", t.id());
        assertEquals("Refund", t.description());
        assertEquals(new BigDecimal("-25.50"), t.amount());
        assertEquals("4111111111111111", t.cardNumber());
    }
}
