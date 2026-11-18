package com.example.demo1.domain.model;

import java.util.UUID;

public class OrderItem {
    private final UUID productId;
    private final int quantity;
    private final Money unitPrice;

    public OrderItem(UUID productId, int quantity, Money unitPrice) {
        if (productId == null || unitPrice == null) {
            throw new IllegalArgumentException("Sản phẩm và đơn giá không được rỗng");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng sản phẩm phải lớn hơn 0");
        }
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // Thành tiền của một mặt hàng = đơn giá nhân số lượng.
    public Money getSubTotal() {
        return unitPrice.multiply(quantity);
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

}
