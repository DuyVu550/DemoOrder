package com.example.demo1.application.service;

import java.util.UUID;

public class OrderNotFoundException extends IllegalArgumentException {
    public OrderNotFoundException(UUID orderId) {
        super("Không tìm thấy đơn hàng: " + orderId);
    }
}
