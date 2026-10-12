package com.aws.carddemo.batch.statement.model;

import java.math.BigDecimal;

/** The fields of an account record (copybook CVACT01Y, RECLN 300) that a statement prints. */
public record Account(long id, BigDecimal currentBalance) {
}
