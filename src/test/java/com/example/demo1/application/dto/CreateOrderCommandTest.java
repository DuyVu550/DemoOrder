package com.example.demo1.application.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateOrderCommandTest {
    @Test
    void rejectsNullItemInRequest() {
        List<CreateOrderCommand.OrderItemCommand> items = new ArrayList<>();
        items.add(null);
        var command = new CreateOrderCommand(UUID.randomUUID(), items);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(command).isEmpty());
        }
    }

    @Test
    void rejectsInvalidNestedItem() {
        var item = new CreateOrderCommand.OrderItemCommand(null, 0, BigDecimal.valueOf(-1));
        var command = new CreateOrderCommand(UUID.randomUUID(), List.of(item));

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(command);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().endsWith("productId")));
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().endsWith("quantity")));
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().endsWith("priceVnd")));
        }
    }

    @Test
    void acceptsValidRequestIncludingZeroPrice() {
        var item = new CreateOrderCommand.OrderItemCommand(UUID.randomUUID(), 1, BigDecimal.ZERO);
        var command = new CreateOrderCommand(UUID.randomUUID(), List.of(item));

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(command).isEmpty());
        }
    }
}
