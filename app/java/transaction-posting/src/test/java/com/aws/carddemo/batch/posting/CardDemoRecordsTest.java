package com.aws.carddemo.batch.posting;

import com.aws.carddemo.batch.posting.io.CardDemoRecords;
import com.aws.carddemo.batch.posting.model.RejectReason;
import com.aws.carddemo.batch.posting.model.RejectedTransaction;
import com.aws.carddemo.batch.posting.model.Transaction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

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
}
