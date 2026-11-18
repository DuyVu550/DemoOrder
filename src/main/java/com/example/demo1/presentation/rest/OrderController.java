package com.example.demo1.presentation.rest;

import com.example.demo1.application.dto.CreateOrderCommand;
import com.example.demo1.application.dto.OrderResponse;
import com.example.demo1.application.service.OrderApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

// Nhận HTTP và trả dữ liệu JSON; nghiệp vụ được giao cho application service.
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, UUID>> createOrder(@Valid @RequestBody CreateOrderCommand command) {
        UUID orderId = orderApplicationService.createOrder(command);
        // Location giúp client tìm lại đơn vừa được tạo.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{orderId}")
                .buildAndExpand(orderId)
                .toUri();
        return ResponseEntity.created(location).body(Map.of("orderId", orderId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderApplicationService.getOrder(orderId));
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<Void> payOrder(@PathVariable UUID orderId) {
        orderApplicationService.payOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID orderId) {
        orderApplicationService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}
