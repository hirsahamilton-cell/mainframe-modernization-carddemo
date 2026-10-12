package com.aws.carddemo.batch.statement.model;

import java.math.BigDecimal;

/** The fields of a posted transaction record (copybook CVTRA05Y, RECLN 350) that a statement prints. */
public record Transaction(String id, String description, BigDecimal amount, String cardNumber) {
}
