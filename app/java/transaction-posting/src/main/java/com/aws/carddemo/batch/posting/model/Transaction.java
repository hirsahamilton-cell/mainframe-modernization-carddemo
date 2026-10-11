package com.aws.carddemo.batch.posting.model;

import java.math.BigDecimal;

/**
 * Transaction record. The daily transaction file (CVTRA06Y) and the transaction master (CVTRA05Y) share
 * this 350-byte layout. Text fields keep their leading spaces; only trailing padding is dropped.
 */
public record Transaction(
        String id,
        String typeCode,
        int categoryCode,
        String source,
        String description,
        BigDecimal amount,
        long merchantId,
        String merchantName,
        String merchantCity,
        String merchantZip,
        String cardNumber,
        String originTimestamp,
        String processedTimestamp) {

    /** First 10 characters of the origin timestamp (YYYY-MM-DD), as compared by 1500-B-LOOKUP-ACCT. */
    public String originDate() {
        return originTimestamp.length() >= 10 ? originTimestamp.substring(0, 10) : originTimestamp;
    }

    public Transaction withProcessedTimestamp(String timestamp) {
        return new Transaction(id, typeCode, categoryCode, source, description, amount, merchantId, merchantName,
                merchantCity, merchantZip, cardNumber, originTimestamp, timestamp);
    }
}
