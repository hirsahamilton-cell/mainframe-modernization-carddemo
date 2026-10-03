package com.aws.carddemo.batch.interest;

import com.aws.carddemo.batch.interest.model.DisclosureGroup;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Interest rate lookup over the disclosure group file (1200-GET-INTEREST-RATE).
 * When no rate exists for the account's group, the {@value #DEFAULT_GROUP} group is used
 * (1200-A-GET-DEFAULT-INT-RATE); if that is missing too, the lookup fails.
 */
public final class InterestRateTable {

    public static final String DEFAULT_GROUP = "DEFAULT";

    private record Key(String groupId, String typeCode, int categoryCode) {
        Key {
            groupId = groupId.strip();
        }
    }

    private final Map<Key, BigDecimal> rates = new HashMap<>();

    public InterestRateTable(Collection<DisclosureGroup> groups) {
        for (DisclosureGroup g : groups) {
            rates.put(new Key(g.accountGroupId(), g.typeCode(), g.categoryCode()), g.annualInterestRate());
        }
    }

    /** Returns the annual percentage rate for the given account group and transaction type/category. */
    public BigDecimal annualRateFor(String accountGroupId, String typeCode, int categoryCode) {
        BigDecimal rate = rates.get(new Key(accountGroupId, typeCode, categoryCode));
        if (rate != null) {
            return rate;
        }
        rate = rates.get(new Key(DEFAULT_GROUP, typeCode, categoryCode));
        if (rate == null) {
            throw new IllegalStateException(String.format(
                    "No disclosure group rate for group '%s' or %s, type %s, category %04d",
                    accountGroupId.strip(), DEFAULT_GROUP, typeCode, categoryCode));
        }
        return rate;
    }
}
