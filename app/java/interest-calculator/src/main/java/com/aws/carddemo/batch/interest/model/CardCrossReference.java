package com.aws.carddemo.batch.interest.model;

/** Card / customer / account cross-reference record (copybook CVACT03Y). */
public record CardCrossReference(String cardNumber, long customerId, long accountId) {
}
