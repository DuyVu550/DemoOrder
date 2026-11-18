package com.example.demo1.application.service;

import com.example.demo1.domain.model.Money;
import com.example.demo1.domain.model.Order;
import com.example.demo1.domain.model.OrderItem;
import com.example.demo1.domain.model.OrderStatus;
import com.example.demo1.domain.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderApplicationServiceMockTest {
    @Test
    void paymentSavesPaidOrder() {
        OrderRepository repository = mock(OrderRepository.class);
        var service = new OrderApplicationService(repository);
        Order order = Order.create(UUID.randomUUID(), List.of(new OrderItem(UUID.randomUUID(), 1, Money.vnd(100))));
        when(repository.findById(order.getId())).thenReturn(Optional.of(order));

        service.payOrder(order.getId());

        var saved = ArgumentCaptor.forClass(Order.class);
        verify(repository).save(saved.capture());
        assertEquals(order.getId(), saved.getValue().getId());
        assertEquals(OrderStatus.PAID, saved.getValue().getStatus());
    }

    @Test
    void missingOrderIsNotSaved() {
        OrderRepository repository = mock(OrderRepository.class);
        var service = new OrderApplicationService(repository);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.payOrder(id));
        verify(repository, never()).save(any());
    }

    @Test
    void cancelledOrderIsNotSavedAgainWhenPaymentIsRejected() {
        OrderRepository repository = mock(OrderRepository.class);
        var service = new OrderApplicationService(repository);
        Order order = Order.create(UUID.randomUUID(), List.of(new OrderItem(UUID.randomUUID(), 1, Money.vnd(100))));
        order.cancel();
        UUID id = order.getId();
        when(repository.findById(id)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> service.payOrder(id));
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(repository, never()).save(any());
    }
}
