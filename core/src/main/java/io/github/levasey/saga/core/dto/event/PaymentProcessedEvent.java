package io.github.levasey.saga.core.dto.event;

import java.util.UUID;

public class PaymentProcessedEvent {
    private UUID orderId;
    private UUID id;

    public PaymentProcessedEvent() {}

    public PaymentProcessedEvent(UUID orderId, UUID id) {
        this.orderId = orderId;
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
