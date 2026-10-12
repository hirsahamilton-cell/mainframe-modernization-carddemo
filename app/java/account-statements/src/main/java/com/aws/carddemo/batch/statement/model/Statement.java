package com.aws.carddemo.batch.statement.model;

import java.math.BigDecimal;
import java.util.List;

/** One card's statement: its customer and account, its transactions in id order and their total. */
public record Statement(CardCrossReference card, Customer customer, Account account,
                        List<Transaction> transactions, BigDecimal total) {
}
