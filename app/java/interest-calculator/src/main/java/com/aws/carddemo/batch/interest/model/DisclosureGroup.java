package com.aws.carddemo.batch.interest.model;

import java.math.BigDecimal;

/**
 * Disclosure group record (copybook CVTRA02Y, RECLN 50).
 *
 * @param annualInterestRate annual percentage rate, e.g. {@code 15.00} for 15%
 */
public record DisclosureGroup(String accountGroupId, String typeCode, int categoryCode, BigDecimal annualInterestRate) {
}
