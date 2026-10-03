package com.aws.carddemo.batch.interest.model;

import java.math.BigDecimal;

/** Transaction record (copybook CVTRA05Y, RECLN 350). */
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
}
