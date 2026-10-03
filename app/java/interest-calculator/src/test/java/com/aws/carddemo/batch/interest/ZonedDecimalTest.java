package com.aws.carddemo.batch.interest;

import com.aws.carddemo.batch.interest.io.ZonedDecimal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZonedDecimalTest {

    @Test
    void decodesOverpunchedSigns() {
        assertEquals(new BigDecimal("194.00"), ZonedDecimal.decode("00000001940{", 2));
        assertEquals(new BigDecimal("1164.87"), ZonedDecimal.decode("0000011648G", 2));
        assertEquals(new BigDecimal("-70.77"), ZonedDecimal.decode("0000000707P", 2));
        assertEquals(new BigDecimal("-763.00"), ZonedDecimal.decode("0000007630}", 2));
        assertEquals(new BigDecimal("15.00"), ZonedDecimal.decode("00150{", 2));
    }

    @Test
    void encodesRoundTrip() {
        for (String v : new String[] {"0.00", "14.56", "-70.77", "29.00", "-0.01", "999999999.99"}) {
            BigDecimal value = new BigDecimal(v);
            assertEquals(value, ZonedDecimal.decode(ZonedDecimal.encode(value, 9, 2), 2));
        }
        assertEquals("0000000145F", ZonedDecimal.encode(new BigDecimal("14.56"), 9, 2));
        assertEquals("0000000290{", ZonedDecimal.encode(new BigDecimal("29.00"), 9, 2));
    }

    @Test
    void rejectsOverflow() {
        assertThrows(IllegalArgumentException.class, () -> ZonedDecimal.encode(new BigDecimal("1000000000.00"), 9, 2));
    }
}
