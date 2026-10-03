package com.aws.carddemo.batch.interest;

import com.aws.carddemo.batch.interest.io.CardDemoRecords;
import com.aws.carddemo.batch.interest.model.DisclosureGroup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterestRateTableTest {

    private static List<DisclosureGroup> groups;
    private static InterestRateTable table;

    @BeforeAll
    static void loadSampleDisclosureGroups() throws IOException {
        groups = CardDemoRecords.readAll(SampleData.ascii("discgrp.txt"), CardDemoRecords::parseDisclosureGroup);
        table = new InterestRateTable(groups);
    }

    @Test
    void usesSpecificGroupWhenPresent() {
        assertEquals(new BigDecimal("15.00"), table.annualRateFor("A000000000", "01", 1));
        assertEquals(new BigDecimal("25.00"), table.annualRateFor("A000000000", "01", 2));
        assertEquals(new BigDecimal("0.00"), table.annualRateFor("ZEROAPR", "01", 1));
        // A specific match wins even where DEFAULT has a different rate (DEFAULT 07/0001 is 0%).
        assertEquals(new BigDecimal("15.00"), table.annualRateFor("A000000000", "07", 1));
        assertEquals(new BigDecimal("0.00"), table.annualRateFor("DEFAULT", "07", 1));
    }

    @Test
    void fallsBackToDefaultGroupWhenNoSpecificMatch() {
        // Sample accounts carry a blank ACCT-GROUP-ID, so every real lookup takes this path.
        assertEquals(new BigDecimal("15.00"), table.annualRateFor("          ", "01", 1));
        assertEquals(new BigDecimal("25.00"), table.annualRateFor("UNKNOWN", "01", 3));
        assertEquals(new BigDecimal("0.00"), table.annualRateFor("UNKNOWN", "07", 1));
    }

    @Test
    void failsWhenNeitherSpecificNorDefaultGroupExists() {
        assertThrows(IllegalStateException.class, () -> table.annualRateFor("A000000000", "09", 1));
    }
}
