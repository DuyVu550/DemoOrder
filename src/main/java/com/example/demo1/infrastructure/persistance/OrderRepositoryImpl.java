package com.example.demo1.infrastructure.persistance;

import com.example.demo1.domain.model.Order;
import com.example.demo1.domain.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderRepositoryImpl implements OrderRepository {
    private final SpringDataOrderRepository jpaRepository;
    private final OrderMapper mapper; // Chuyển đổi qua lại giữa Order (Domain) và OrderJpaEntity (DB)

    public OrderRepositoryImpl(SpringDataOrderRepository jpaRepository, OrderMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = mapper.toJpaEntity(order);
        OrderJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomainEntity(saved);
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return jpaRepository.findById(orderId)
                .map(mapper::toDomainEntity);
    }
}
