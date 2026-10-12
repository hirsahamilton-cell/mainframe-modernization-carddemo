package com.aws.carddemo.batch.report.model;

/**
 * Inclusive range of processed dates (YYYY-MM-DD) to report on: the DATEPARM record and the SORT INCLUDE
 * condition of TRANREPT.jcl. Dates are compared as text, like the SORT and CBTRN03C do.
 */
public record DateRange(String start, String end) {

    public boolean contains(String date) {
        return date.compareTo(start) >= 0 && date.compareTo(end) <= 0;
    }
}
