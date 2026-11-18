package com.example.demo1.application.service;

import com.example.demo1.application.dto.CreateOrderCommand;
import com.example.demo1.application.dto.OrderResponse;
import com.example.demo1.domain.model.Money;
import com.example.demo1.domain.model.Order;
import com.example.demo1.domain.model.OrderItem;
import com.example.demo1.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderApplicationService {
    private final OrderRepository orderRepository;

    public OrderApplicationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Use Case 1: Tạo đơn hàng mới
    @Transactional
    public UUID createOrder(CreateOrderCommand command) {
        List<OrderItem> domainItems = command.items().stream()
                .map(item -> new OrderItem(
                        item.productId(),
                        item.quantity(),
                        Money.vnd(item.priceVnd())
                ))
                .toList();

        // Gọi Factory của Domain
        Order newOrder = Order.create(command.customerId(), domainItems);

        // Lưu xuống DB thông qua interface
        orderRepository.save(newOrder);
        return newOrder.getId();
    }

    // Giữ transaction khi đọc và chuyển dữ liệu sang response.
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return OrderResponse.from(findOrder(orderId));
    }

    // Use Case 2: Thanh toán đơn hàng
    @Transactional
    public void payOrder(UUID orderId) {
        Order order = findOrder(orderId);

        // Giao toàn quyền quyết định logic cho Domain Object
        order.pay();

        orderRepository.save(order);
    }

    // Use Case 3: Hủy đơn hàng
    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = findOrder(orderId);

        order.cancel();

        orderRepository.save(order);
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
