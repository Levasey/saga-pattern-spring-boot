package io.github.levasey.saga.orders.service;

import io.github.levasey.saga.core.dto.Order;

import java.util.UUID;

public interface OrderService {
    Order placeOrder(Order order);

    void approveOrder(UUID orderId);

    void rejectOrder(UUID orderId);
}
