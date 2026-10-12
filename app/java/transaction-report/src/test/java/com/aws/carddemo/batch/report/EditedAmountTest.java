package com.aws.carddemo.batch.report;

import com.aws.carddemo.batch.report.io.EditedAmount;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Expected strings are taken from the GnuCOBOL run of CBTRN03C (cobol-baseline/edge). */
class EditedAmountTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0.00         |'               '|'               '",
            "0.05         |'            .05'|'+           .05'",
            "-0.05        |'-           .05'|'-           .05'",
            "42.00        |'          42.00'|'+         42.00'",
            "1453.87      |'       1,453.87'|'+      1,453.87'",
            "-1234567.89  |'-  1,234,567.89'|'-  1,234,567.89'",
            "999999999.99 |' 999,999,999.99'|'+999,999,999.99'",
    })
    void formatsLikeTheCobolPictures(String amount, String detail, String total) {
        assertEquals(detail, EditedAmount.detail(new BigDecimal(amount)));
        assertEquals(total, EditedAmount.total(new BigDecimal(amount)));
    }

    @Test
    void rejectsAmountsThatDoNotFit() {
        assertThrows(IllegalArgumentException.class, () -> EditedAmount.total(new BigDecimal("1000000000.00")));
    }
}
