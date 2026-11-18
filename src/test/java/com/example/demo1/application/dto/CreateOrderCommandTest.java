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

    @Test
    void validatesPricePrecisionAndDatabaseLimit() {
        UUID product = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        var max = new CreateOrderCommand(customer, List.of(
                new CreateOrderCommand.OrderItemCommand(product, 1, new BigDecimal("99999999999999999.99"))));
        var overflow = new CreateOrderCommand(customer, List.of(
                new CreateOrderCommand.OrderItemCommand(product, 1, new BigDecimal("100000000000000000.00"))));
        var fractional = new CreateOrderCommand(customer, List.of(
                new CreateOrderCommand.OrderItemCommand(product, 1, new BigDecimal("0.001"))));

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(max).isEmpty());
            assertFalse(validator.validate(overflow).isEmpty());
            assertFalse(validator.validate(fractional).isEmpty());
        }
    }

    @Test
    void rejectsMissingCustomerAndMissingItems() {
        var item = new CreateOrderCommand.OrderItemCommand(UUID.randomUUID(), 1, BigDecimal.ONE);
        var noCustomer = new CreateOrderCommand(null, List.of(item));
        var noItems = new CreateOrderCommand(UUID.randomUUID(), null);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(noCustomer).isEmpty());
            assertFalse(validator.validate(noItems).isEmpty());
        }
    }
}
