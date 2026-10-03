package com.aws.carddemo.batch.interest;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

final class SampleData {

    static final String PARM_DATE = "2022071800";
    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2022-07-18T01:02:03.45Z"), ZoneOffset.UTC);

    private SampleData() {
    }

    /** The shipped CardDemo ASCII sample files (app/data/ASCII). */
    static Path ascii(String fileName) {
        return Path.of(System.getProperty("carddemo.data.dir", "../../data/ASCII")).resolve(fileName);
    }

    /** Inputs and outputs of the original COBOL run, produced by cobol-harness/run-cobol-baseline.sh. */
    static Path cobolBaseline(String fileName) {
        try {
            return Path.of(SampleData.class.getResource("/cobol-baseline/" + fileName).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
