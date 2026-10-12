package com.aws.carddemo.batch.statement.model;

/**
 * The fields of a customer record (copybook CUSTREC, RECLN 500) that a statement prints. Text fields have their
 * trailing spaces removed; {@code ficoScore} keeps its three digits as stored.
 */
public record Customer(
        long id,
        String firstName,
        String middleName,
        String lastName,
        String addressLine1,
        String addressLine2,
        String addressLine3,
        String stateCode,
        String countryCode,
        String zip,
        String ficoScore) {
}
