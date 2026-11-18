package com.example.demo1.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// @Valid kiểm tra tiếp từng item; @NotNull ngăn phần tử null lọt qua validation.
public record CreateOrderCommand(
        @NotNull UUID customerId,
        @NotEmpty List<@NotNull @Valid OrderItemCommand> items) {

    public record OrderItemCommand(
            @NotNull UUID productId,
            @Positive int quantity,
            // Khớp NUMERIC(19,2) trong database để tránh làm tròn đơn giá khi lưu.
            @NotNull @DecimalMin(value = "0.0", inclusive = true) @Digits(integer = 17, fraction = 2) BigDecimal priceVnd) {
    }
}
