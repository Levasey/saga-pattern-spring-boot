package io.github.levasey.saga.orders.service;

import io.github.levasey.saga.core.types.OrderStatus;
import io.github.levasey.saga.orders.dto.OrderHistory;

import java.util.List;
import java.util.UUID;

public interface OrderHistoryService {
    void add(UUID orderId, OrderStatus orderStatus);

    List<OrderHistory> findByOrderId(UUID orderId);
}
