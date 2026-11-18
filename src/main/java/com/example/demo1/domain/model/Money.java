package com.example.demo1.domain.model;

import java.math.BigDecimal;

// BigDecimal tránh sai số nhị phân khi tính tiền; currency ngăn cộng khác loại tiền.
public record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số tiền không được âm hoặc rỗng");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Đơn vị tiền tệ không hợp lệ");
        }
    }

    public static Money vnd(long amount) {
        return new Money(BigDecimal.valueOf(amount), "VND");
    }

    public static Money vnd(BigDecimal amount) {
        return new Money(amount, "VND");
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Không thể cộng hai loại tiền tệ khác nhau");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }


}
