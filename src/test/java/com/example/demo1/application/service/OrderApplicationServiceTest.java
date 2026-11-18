package com.example.demo1.application.service;

import com.example.demo1.application.dto.CreateOrderCommand;
import com.example.demo1.domain.model.Order;
import com.example.demo1.domain.model.Money;
import com.example.demo1.domain.model.OrderItem;
import com.example.demo1.domain.model.OrderStatus;
import com.example.demo1.domain.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderApplicationServiceTest {

    @Test
    void createsAndPersistsPendingOrderFromCommand() {
        var repository = new InMemoryOrderRepository();
        var service = new OrderApplicationService(repository);
        var customerId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var command = new CreateOrderCommand(customerId, List.of(
                new CreateOrderCommand.OrderItemCommand(productId, 2, new BigDecimal("15000")))
        );

        UUID orderId = service.createOrder(command);

        assertNotNull(orderId);
        Order savedOrder = repository.findById(orderId).orElseThrow();
        assertEquals(customerId, savedOrder.getCustomerId());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        assertEquals(productId, savedOrder.getItems().get(0).getProductId());
        assertEquals(2, savedOrder.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("30000"), savedOrder.getTotalPrice().amount());
    }

    @Test
    void returnsOrderDetailsIncludingItemsAndTotal() {
        var repository = new InMemoryOrderRepository();
        var service = new OrderApplicationService(repository);
        Order order = sampleOrder();
        repository.save(order);

        var response = service.getOrder(order.getId());

        assertEquals(order.getId(), response.id());
        assertEquals(order.getCustomerId(), response.customerId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(order.getCreatedAt(), response.createdAt());
        assertEquals(new BigDecimal("30000"), response.totalPriceVnd());
        assertEquals(1, response.items().size());
        assertEquals(order.getItems().get(0).getProductId(), response.items().get(0).productId());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(new BigDecimal("15000"), response.items().get(0).unitPriceVnd());
    }

    @Test
    void persistsPayment() {
        var repository = new InMemoryOrderRepository();
        var service = new OrderApplicationService(repository);
        Order order = sampleOrder();
        repository.save(order);

        service.payOrder(order.getId());

        assertEquals(OrderStatus.PAID, service.getOrder(order.getId()).status());
    }

    @Test
    void persistsCancellation() {
        var repository = new InMemoryOrderRepository();
        var service = new OrderApplicationService(repository);
        Order order = sampleOrder();
        repository.save(order);

        service.cancelOrder(order.getId());

        assertEquals(OrderStatus.CANCELLED, service.getOrder(order.getId()).status());
    }

    @Test
    void rejectsMissingOrderForReadPaymentAndCancellation() {
        var service = new OrderApplicationService(new InMemoryOrderRepository());
        UUID missingId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> service.getOrder(missingId));
        assertThrows(IllegalArgumentException.class, () -> service.payOrder(missingId));
        assertThrows(IllegalArgumentException.class, () -> service.cancelOrder(missingId));
    }

    @Test
    void rejectsPaymentOfCancelledOrderWithoutChangingStoredState() {
        var repository = new InMemoryOrderRepository();
        var service = new OrderApplicationService(repository);
        Order order = sampleOrder();
        repository.save(order);
        service.cancelOrder(order.getId());

        UUID orderId = order.getId();
        assertThrows(IllegalStateException.class, () -> service.payOrder(orderId));
        assertEquals(OrderStatus.CANCELLED, service.getOrder(order.getId()).status());
    }

    @Test
    void rejectsInvalidOrderWithoutPersistingIt() {
        var repository = new InMemoryOrderRepository();
        var service = new OrderApplicationService(repository);
        var command = new CreateOrderCommand(UUID.randomUUID(), List.of(
                new CreateOrderCommand.OrderItemCommand(UUID.randomUUID(), 0, BigDecimal.TEN))
        );

        assertThrows(IllegalArgumentException.class, () -> service.createOrder(command));
        assertEquals(0, repository.saves);
    }

    private static Order sampleOrder() {
        return Order.create(UUID.randomUUID(), List.of(
                new OrderItem(UUID.randomUUID(), 2, Money.vnd(15000)))
        );
    }

    private static final class InMemoryOrderRepository implements OrderRepository {
        private Order order;
        private int saves;

        @Override
        public Order save(Order order) {
            this.order = copy(order);
            saves++;
            return order;
        }

        @Override
        public Optional<Order> findById(UUID orderId) {
            return order != null && order.getId().equals(orderId) ? Optional.of(copy(order)) : Optional.empty();
        }

        // Return detached copies so missing save() calls cannot pass persistence tests.
        private static Order copy(Order order) {
            return Order.reconstruct(order.getId(), order.getCustomerId(), order.getItems(),
                    order.getTotalPrice(), order.getStatus(), order.getCreatedAt()
            );
        }
    }
}
