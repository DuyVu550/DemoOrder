package com.example.demo1.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Spring Data tự cung cấp các thao tác JPA như save và findById.
public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {
}
