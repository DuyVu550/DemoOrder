package com.example.demo1.domain.repository;

import com.example.demo1.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

// Domain chỉ định nghĩa thao tác cần dùng, không phụ thuộc JPA hay PostgreSQL.
public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(UUID orderId);
}
