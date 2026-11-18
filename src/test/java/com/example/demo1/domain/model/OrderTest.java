package com.example.demo1.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {
    @Test
    void createsPendingOrderWithTotalAcrossAllItems() {
        Order order = Order.create(UUID.randomUUID(), List.of(item(2, 15000), item(3, 10000)));
        assertNotNull(order.getId());
        assertNotNull(order.getCreatedAt());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(Money.vnd(60000), order.getTotalPrice());
    }

    @Test
    void rejectsMissingCustomer() {
        List<OrderItem> items = List.of(item(1, 100));
        assertThrows(IllegalArgumentException.class, () -> Order.create(null, items));
    }

    @Test
    void rejectsMissingOrEmptyItems() {
        UUID customerId = UUID.randomUUID();
        List<OrderItem> emptyItems = List.of();
        assertThrows(IllegalStateException.class, () -> Order.create(customerId, null));
        assertThrows(IllegalStateException.class, () -> Order.create(customerId, emptyItems));
    }

    @Test
    void rejectsNullItem() {
        UUID customerId = UUID.randomUUID();
        List<OrderItem> items = new ArrayList<>();
        items.add(null);
        assertThrows(IllegalArgumentException.class, () -> Order.create(customerId, items));
    }

    @Test
    void protectsItemsFromExternalChanges() {
        var items = new ArrayList<>(List.of(item(1, 100)));
        Order order = Order.create(UUID.randomUUID(), items);
        items.clear();
        assertEquals(1, order.getItems().size());
        assertEquals(Money.vnd(100), order.getTotalPrice());
        List<OrderItem> exposedItems = order.getItems();
        assertThrows(UnsupportedOperationException.class, exposedItems::clear);
    }

    @Test
    void paysPendingOrder() {
        Order order = Order.create(UUID.randomUUID(), List.of(item(1, 100)));
        order.pay();
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PAID", "CANCELLED", "SHIPPED", "DELIVERED"})
    void rejectsPaymentOutsidePendingState(OrderStatus status) {
        Order order = restored(status);
        assertThrows(IllegalStateException.class, order::pay);
        assertEquals(status, order.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "PAID", "CANCELLED"})
    void cancelsOrderBeforeShipping(OrderStatus status) {
        Order order = restored(status);
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"SHIPPED", "DELIVERED"})
    void rejectsCancellationAfterShipping(OrderStatus status) {
        Order order = restored(status);
        assertThrows(IllegalStateException.class, order::cancel);
        assertEquals(status, order.getStatus());
    }

    @Test
    void reconstructsOriginalCreationTime() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 15, 10, 30);
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Order order = Order.reconstruct(id, customerId, List.of(item(1, 100)),
                Money.vnd(100), OrderStatus.PAID, createdAt);

        assertEquals(createdAt, order.getCreatedAt());
        assertEquals(id, order.getId());
        assertEquals(customerId, order.getCustomerId());
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(Money.vnd(100), order.getTotalPrice());
    }

    private static OrderItem item(int quantity, long price) {
        return new OrderItem(UUID.randomUUID(), quantity, Money.vnd(price));
    }



    private static Order restored(OrderStatus status) {
        return Order.reconstruct(UUID.randomUUID(), UUID.randomUUID(), List.of(item(1, 100)),
                Money.vnd(100), status, LocalDateTime.of(2026, 1, 15, 10, 30));
    }
}
