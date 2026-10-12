package com.aws.carddemo.batch.report.model;

/** Transaction type record (copybook CVTRA03Y, RECLN 60). */
public record TransactionType(String code, String description) {
}
