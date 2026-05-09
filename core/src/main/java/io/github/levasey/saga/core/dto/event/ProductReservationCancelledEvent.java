package io.github.levasey.saga.core.dto.event;

import java.util.UUID;

public class ProductReservationCancelledEvent {
    private UUID orderId;
    private UUID productId;

    public ProductReservationCancelledEvent() {
    }

    public ProductReservationCancelledEvent(UUID orderId, UUID productId) {
        this.orderId = orderId;
        this.productId = productId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }
}
