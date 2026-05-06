package io.github.levasey.saga.orders.service;

import io.github.levasey.saga.core.dto.Order;

public interface OrderService {
    Order placeOrder(Order order);
}
