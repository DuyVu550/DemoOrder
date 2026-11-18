package com.example.demo1.infrastructure.persistance;

import com.example.demo1.domain.model.OrderStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {
    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    // Các item được lưu trong bảng riêng và nạp cùng đơn để mapper đọc được.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "orders_id"))
    @AttributeOverride(name = "productId", column = @Column(name = "product_id", nullable = false))
    @AttributeOverride(name = "quantity", column = @Column(name = "quantity", nullable = false))
    @AttributeOverride(name = "unitPriceVnd", column = @Column(name = "unit_price_vnd", nullable = false, precision = 19, scale = 2))
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    @Column(name = "total_price_vnd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPriceVnd;

    // Lưu tên trạng thái thay vì vị trí enum để tránh lệch khi thay đổi thứ tự.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected OrderJpaEntity() {
        // Required by JPA to hydrate entities from database rows.
    }

    public OrderJpaEntity(UUID id, UUID customerId, List<OrderItemJpaEntity> items, BigDecimal totalPriceVnd,
                          OrderStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.totalPriceVnd = totalPriceVnd;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public List<OrderItemJpaEntity> getItems() {
        return items;
    }

    public BigDecimal getTotalPriceVnd() {
        return totalPriceVnd;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
