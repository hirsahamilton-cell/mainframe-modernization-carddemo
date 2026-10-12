package com.aws.carddemo.batch.report.model;

import java.math.BigDecimal;

/** The fields of a posted transaction record (copybook CVTRA05Y, RECLN 350) that the report uses. */
public record Transaction(
        String id,
        String typeCode,
        int categoryCode,
        String source,
        BigDecimal amount,
        String cardNumber,
        String processedTimestamp) {

    /** First 10 characters of the processed timestamp (YYYY-MM-DD), as compared by TRANREPT. */
    public String processedDate() {
        return processedTimestamp.length() >= 10 ? processedTimestamp.substring(0, 10) : processedTimestamp;
    }
}
