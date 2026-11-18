package com.example.demo1.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderItemTest {
    @Test
    void calculatesSubtotalFromQuantityAndUnitPrice() {
        var item = new OrderItem(UUID.randomUUID(), 3, Money.vnd(new BigDecimal("12000.50")));
        assertEquals(new BigDecimal("36001.50"), item.getSubTotal().amount());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveQuantity(int quantity) {
        UUID productId = UUID.randomUUID();
        Money price = Money.vnd(100);
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem(productId, quantity, price));
    }

    @Test
    void rejectsMissingProduct() {
        Money price = Money.vnd(100);
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(null, 1, price));
    }

    @Test
    void rejectsMissingUnitPrice() {
        UUID productId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(productId, 1, null));
    }
}
