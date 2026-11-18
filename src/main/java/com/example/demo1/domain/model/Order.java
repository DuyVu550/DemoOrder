package com.example.demo1.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Order {
    private final UUID id;
    private final UUID customerId;
    private final List<OrderItem> items;
    private Money totalPrice;
    private OrderStatus status;
    private final LocalDateTime createdAt;

    // Constructor private để ép buộc khởi tạo qua Factory Method
    private Order(UUID id, UUID customerId, List<OrderItem> items, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.PENDING;
        this.createdAt = createdAt;
        this.recalculateTotal();
    }

    // Factory Method tạo mới đơn hàng (Chứa luật nghiệp vụ khởi tạo)
    public static Order create(UUID customerId, List<OrderItem> items) {
        if (customerId == null) {
            throw new IllegalArgumentException("Đơn hàng phải có khách hàng");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Không thể tạo đơn hàng rỗng (không có sản phẩm)");
        }
        if (items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Danh sách sản phẩm không được chứa phần tử rỗng");
        }
        return new Order(UUID.randomUUID(), customerId, items, LocalDateTime.now());
    }

    // Hành vi nghiệp vụ 1: Thanh toán đơn hàng
    public void pay() {
        if (this.status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Chỉ có thể thanh toán đơn hàng đang ở trạng thái PENDING");
        }
        this.status = OrderStatus.PAID;
    }

    // Hành vi nghiệp vụ 2: Hủy đơn hàng
    public void cancel() {
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Không thể hủy đơn hàng đã giao cho đơn vị vận chuyển");
        }
        this.status = OrderStatus.CANCELLED;
    }

    // Logic nội tại: Tự động tính tổng tiền từ các items
    private void recalculateTotal() {
        this.totalPrice = items.stream()
                .map(OrderItem::getSubTotal)
                .reduce(Money.vnd(0), Money::add);
    }

    // Chỉ cung cấp Getters, bảo vệ danh sách items bằng unmodifiableList
    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Money getTotalPrice() {
        return totalPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Hàm phục hồi dữ liệu từ Database (dùng cho tầng Infrastructure)
    public static Order reconstruct(UUID id, UUID customerId, List<OrderItem> items, Money totalPrice, OrderStatus status, LocalDateTime createdAt) {
        // Dùng dữ liệu đã lưu, đặc biệt là createdAt, thay vì tạo thời gian mới.
        Order order = new Order(id, customerId, items, createdAt);
        order.totalPrice = totalPrice;
        order.status = status;
        return order;
    }
}
