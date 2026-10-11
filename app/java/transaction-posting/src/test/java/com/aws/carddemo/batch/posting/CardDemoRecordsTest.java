package com.aws.carddemo.batch.posting;

import com.aws.carddemo.batch.posting.io.CardDemoRecords;
import com.aws.carddemo.batch.posting.model.Account;
import com.aws.carddemo.batch.posting.model.RejectReason;
import com.aws.carddemo.batch.posting.model.RejectedTransaction;
import com.aws.carddemo.batch.posting.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.aws.carddemo.batch.posting.SampleData.ascii;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CardDemoRecordsTest {

    @Test
    void dailyTransactionsRoundTripUnchanged() throws IOException {
        List<String> lines = Files.readAllLines(ascii("dailytran.txt"), StandardCharsets.US_ASCII);
        assertEquals(300, lines.size());
        for (String line : lines) {
            String formatted = CardDemoRecords.formatTransaction(CardDemoRecords.parseTransaction(line));
            assertEquals(350, formatted.length());
            assertEquals(line.stripTrailing(), formatted.stripTrailing());
        }
    }

    @Test
    void parsesTheCopybookFields() throws IOException {
        Transaction t = CardDemoRecords.parseTransaction(Files.readAllLines(ascii("dailytran.txt")).get(0));
        assertEquals("0000000000683580", t.id());
        assertEquals("01", t.typeCode());
        assertEquals(1, t.categoryCode());
        assertEquals("POS TERM", t.source());
        assertEquals(new BigDecimal("504.77"), t.amount());
        assertEquals("4859452612877065", t.cardNumber());
        assertEquals("2022-06-10", t.originDate());
    }

    @Test
    void rejectRecordsAre430BytesWithTheReasonTrailer() throws IOException {
        Transaction t = CardDemoRecords.parseTransaction(Files.readAllLines(ascii("dailytran.txt")).get(0));
        String reject = CardDemoRecords.formatRejectedTransaction(new RejectedTransaction(t, RejectReason.OVER_LIMIT));
        assertEquals(430, reject.length());
        assertEquals("0102OVERLIMIT TRANSACTION", reject.substring(350).stripTrailing());
    }

    @Test
    void writtenFilesKeepFixedRecordWidths(@TempDir Path dir) throws Exception {
        List<String> daily = Files.readAllLines(ascii("dailytran.txt"), StandardCharsets.US_ASCII);
        Transaction t = CardDemoRecords.parseTransaction(daily.get(0));
        List<Account> accounts =
                CardDemoRecords.readAll(ascii("acctdata.txt"), CardDemoRecords::parseAccount);
        Path transact = dir.resolve("transact.txt");
        Path rejects = dir.resolve("rejects.txt");
        Path accts = dir.resolve("acct.txt");
        CardDemoRecords.writeAll(Map.of(
                transact, CardDemoRecords.formatAll(List.of(t), CardDemoRecords::formatTransaction),
                rejects, CardDemoRecords.formatAll(List.of(new RejectedTransaction(t, RejectReason.OVER_LIMIT)),
                        CardDemoRecords::formatRejectedTransaction),
                accts, CardDemoRecords.formatAll(accounts, CardDemoRecords::formatAccount)));

        assertEquals(List.of(350), Files.readAllLines(transact).stream().map(String::length).distinct().toList());
        assertEquals(List.of(430), Files.readAllLines(rejects).stream().map(String::length).distinct().toList());
        assertEquals(List.of(300), Files.readAllLines(accts).stream().map(String::length).distinct().toList());
        try (var files = Files.list(dir)) {
            assertEquals(3, files.count());
        }
    }
}
