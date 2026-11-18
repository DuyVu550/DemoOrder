package com.example.demo1.infrastructure.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class OrderItemJpaEntity {
    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPriceVnd;

    protected OrderItemJpaEntity() {
        // Required by JPA to hydrate embedded order items.
    }

    public OrderItemJpaEntity(UUID productId, int quantity, BigDecimal unitPriceVnd) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPriceVnd = unitPriceVnd;
    }

    public UUID getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPriceVnd() { return unitPriceVnd; }
}
