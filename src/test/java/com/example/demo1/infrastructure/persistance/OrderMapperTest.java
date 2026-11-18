package com.example.demo1.infrastructure.persistance;

import com.example.demo1.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderMapperTest {
    @Test
    void preservesOrderAndItemDataAcrossMapping() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2025, 2, 3, 10, 15);
        var entity = new OrderJpaEntity(id, customerId,
                List.of(new OrderItemJpaEntity(productId, 3, new BigDecimal("12.50"))),
                new BigDecimal("37.50"), OrderStatus.PAID, createdAt);

        var mapper = new OrderMapper();
        var restored = mapper.toDomainEntity(entity);
        var saved = mapper.toJpaEntity(restored);

        assertEquals(id, saved.getId());
        assertEquals(customerId, saved.getCustomerId());
        assertEquals(createdAt, saved.getCreatedAt());
        assertEquals(OrderStatus.PAID, saved.getStatus());
        assertEquals(new BigDecimal("37.50"), saved.getTotalPriceVnd());
        assertEquals(1, saved.getItems().size());
        assertEquals(productId, saved.getItems().get(0).getProductId());
        assertEquals(3, saved.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("12.50"), saved.getItems().get(0).getUnitPriceVnd());
    }
}
