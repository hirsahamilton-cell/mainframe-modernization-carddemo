package com.aws.carddemo.batch.card.model;

public record Card(
        String cardNumber,
        long accountId,
        int cvv,
        String embossedName,
        String expirationDate,
        String activeStatus,
        String reserved) {
}
