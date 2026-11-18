package com.example.demo1.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {
    @Test
    void addsDecimalAmountsExactlyWithoutChangingOperands() {
        Money first = Money.vnd(new BigDecimal("0.1"));
        Money second = Money.vnd(new BigDecimal("0.2"));

        assertEquals(new Money(new BigDecimal("0.3"), "VND"), first.add(second));
        assertEquals(new BigDecimal("0.1"), first.amount());
        assertEquals(new BigDecimal("0.2"), second.amount());
    }

    @Test
    void multipliesByQuantityAndAcceptsZero() {
        Money price = Money.vnd(new BigDecimal("12500.50"));
        assertEquals(new BigDecimal("37501.50"), price.multiply(3).amount());
        assertEquals(0, price.multiply(0).amount().compareTo(BigDecimal.ZERO));
        assertEquals("VND", price.multiply(3).currency());
    }

    @Test
    void rejectsMissingOrNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> Money.vnd((BigDecimal) null));
        assertThrows(IllegalArgumentException.class, () -> Money.vnd(-1));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsBlankCurrency(String currency) {
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.ZERO, currency));
    }

    @Test
    void rejectsAddingDifferentCurrencies() {
        Money vnd = Money.vnd(100);
        Money usd = new Money(BigDecimal.ONE, "USD");
        assertThrows(IllegalArgumentException.class,
                () -> vnd.add(usd));
    }
}
