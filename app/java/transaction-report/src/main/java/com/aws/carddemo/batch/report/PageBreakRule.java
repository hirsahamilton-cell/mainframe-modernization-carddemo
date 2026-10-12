package com.aws.carddemo.batch.report;

/**
 * Decides, before each detail line, whether to close the current page (page total) and start a new one
 * (headers). {@code lineCounter} counts every report line written so far, as WS-LINE-COUNTER does.
 */
@FunctionalInterface
public interface PageBreakRule {

    int PAGE_SIZE = 20;

    /**
     * A new page starts at the first detail line after the line counter reaches the next multiple of
     * {@link #PAGE_SIZE}. CBTRN03C only checks {@code MOD(WS-LINE-COUNTER, 20) = 0}, so when a two-line
     * account total jumps over the multiple (19 to 21) no break happens until the multiple after that.
     */
    PageBreakRule EVERY_20_LINES = (lineCounter, lineCounterAtLastBreak) ->
            lineCounter / PAGE_SIZE > lineCounterAtLastBreak / PAGE_SIZE;

    boolean newPageDue(long lineCounter, long lineCounterAtLastBreak);
}
