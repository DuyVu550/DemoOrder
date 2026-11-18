package com.example.demo1.infrastructure.persistance;

import com.example.demo1.domain.model.Money;
import com.example.demo1.domain.model.Order;
import com.example.demo1.domain.model.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    // Tách mô hình nghiệp vụ khỏi cấu trúc bảng của JPA.
    public OrderJpaEntity toJpaEntity(Order order) {
        return new OrderJpaEntity(
                order.getId(),
                order.getCustomerId(),
                order.getItems().stream()
                        .map(item -> new OrderItemJpaEntity(item.getProductId(), item.getQuantity(), item.getUnitPrice().amount()))
                        .toList(),
                order.getTotalPrice().amount(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }

    // Khôi phục trạng thái và thời gian tạo đã lưu khi đọc từ database.
    public Order toDomainEntity(OrderJpaEntity entity) {
        return Order.reconstruct(
                entity.getId(),
                entity.getCustomerId(),
                entity.getItems().stream()
                        .map(item -> new OrderItem(item.getProductId(), item.getQuantity(), Money.vnd(item.getUnitPriceVnd())))
                        .toList(),
                Money.vnd(entity.getTotalPriceVnd()),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
