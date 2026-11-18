package com.example.demo1.application.dto;

import com.example.demo1.domain.model.Order;
import com.example.demo1.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        List<OrderItemResponse> items,
        BigDecimal totalPriceVnd,
        OrderStatus status,
        LocalDateTime createdAt) {

    // Chuyển domain sang DTO để API không trả trực tiếp đối tượng nghiệp vụ.
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getItems().stream()
                        .map(item -> new OrderItemResponse(item.getProductId(), item.getQuantity(), item.getUnitPrice().amount()))
                        .toList(),
                order.getTotalPrice().amount(),
                order.getStatus(),
                order.getCreatedAt());
    }

    public record OrderItemResponse(UUID productId, int quantity, BigDecimal unitPriceVnd) {
    }
}
