package com.aws.carddemo.batch.xref.model;

public record CardCrossReference(
        String cardNumber,
        long customerId,
        long accountId,
        String filler) {
}
