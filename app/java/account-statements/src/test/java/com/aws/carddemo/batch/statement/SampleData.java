package com.aws.carddemo.batch.statement;

import com.aws.carddemo.batch.statement.io.CardDemoRecords;
import com.aws.carddemo.batch.statement.model.Account;
import com.aws.carddemo.batch.statement.model.CardCrossReference;
import com.aws.carddemo.batch.statement.model.Customer;
import com.aws.carddemo.batch.statement.model.Transaction;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

final class SampleData {

    private SampleData() {
    }

    /** The shipped CardDemo ASCII sample files (app/data/ASCII). */
    static Path ascii(String fileName) {
        return Path.of(System.getProperty("carddemo.data.dir", "../../data/ASCII")).resolve(fileName);
    }

    static Path resource(String path) {
        try {
            return Path.of(SampleData.class.getResource("/" + path).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Outputs of the original COBOL run, produced by cobol-harness/run-cobol-baseline.sh. */
    static Path cobolBaseline(String scenario, String fileName) {
        return resource("cobol-baseline/" + scenario + "/" + fileName);
    }

    /**
     * "sample" is the COBOL POSTTRAN output for the shipped daily transactions with the shipped cross-reference
     * and customers and the accounts as POSTTRAN left them. "edge" is hand-made: a card with 12 transactions,
     * zero/five-cent/largest/negative amounts, a negative balance, blank and multi-word names, HTML special
     * characters, a 100-character description, two cards on one account, a card without transactions and a
     * transaction on a card that is not in the cross-reference.
     */
    record Scenario(Path transactFile, Path xrefFile, Path acctFile, Path custFile) {

        static Scenario load(String name) {
            return name.equals("sample")
                    ? new Scenario(resource("scenarios/sample-transact.txt"), ascii("cardxref.txt"),
                            resource("scenarios/sample-acctdata.txt"), ascii("custdata.txt"))
                    : new Scenario(resource("scenarios/edge-transact.txt"), resource("scenarios/edge-cardxref.txt"),
                            resource("scenarios/edge-acctdata.txt"), resource("scenarios/edge-custdata.txt"));
        }

        List<Transaction> transactions() throws IOException {
            return CardDemoRecords.readAll(transactFile, CardDemoRecords::parseTransaction);
        }

        List<CardCrossReference> cards() throws IOException {
            return CardDemoRecords.readAll(xrefFile, CardDemoRecords::parseCrossReference);
        }

        List<Customer> customers() throws IOException {
            return CardDemoRecords.readAll(custFile, CardDemoRecords::parseCustomer);
        }

        List<Account> accounts() throws IOException {
            return CardDemoRecords.readAll(acctFile, CardDemoRecords::parseAccount);
        }

        StatementJob job() throws IOException {
            return new StatementJob(cards(), customers(), accounts());
        }

        String[] args(Path statementFile, Path htmlFile) {
            return new String[] {transactFile.toString(), xrefFile.toString(), acctFile.toString(),
                    custFile.toString(), statementFile.toString(), htmlFile.toString()};
        }
    }
}
