package com.aws.carddemo.batch.report;

import com.aws.carddemo.batch.report.io.CardDemoRecords;
import com.aws.carddemo.batch.report.model.DateRange;
import com.aws.carddemo.batch.report.model.Transaction;
import com.aws.carddemo.batch.report.model.TransactionCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardDemoRecordsTest {

    @Test
    void parsesPostedTransactions() throws IOException {
        List<Transaction> transactions = CardDemoRecords.readAll(
                SampleData.resource("scenarios/sample-transact.txt"), CardDemoRecords::parseTransaction);
        assertEquals(262, transactions.size());
        Transaction first = transactions.get(0);
        assertEquals("0000000000683580", first.id());
        assertEquals("01", first.typeCode());
        assertEquals(1, first.categoryCode());
        assertEquals("POS TERM", first.source());
        assertEquals(new BigDecimal("504.77"), first.amount());
        assertEquals("4859452612877065", first.cardNumber());
        assertEquals("2026-10-11", first.processedDate());
    }

    @Test
    void parsesShippedCategoriesIncludingInterestAndFee() throws IOException {
        List<TransactionCategory> categories = CardDemoRecords.readAll(
                SampleData.ascii("trancatg.txt"), CardDemoRecords::parseTransactionCategory);
        assertEquals("Interest Amount", find(categories, "010005").description());
        assertEquals("Over-limit Fee", find(categories, "010006").description());
    }

    @Test
    void readsTheDateParmRecord(@TempDir Path dir) throws IOException {
        Path dateParm = Files.writeString(dir.resolve("dateparm"), "2022-01-01 2022-07-06" + " ".repeat(59));
        assertEquals(Optional.of(new DateRange("2022-01-01", "2022-07-06")), CardDemoRecords.readDateRange(dateParm));
        assertEquals(Optional.empty(), CardDemoRecords.readDateRange(Files.writeString(dir.resolve("empty"), "")));
    }

    private static TransactionCategory find(List<TransactionCategory> categories, String key) {
        return categories.stream().filter(c -> c.key().equals(key)).findFirst().orElseThrow();
    }
}
