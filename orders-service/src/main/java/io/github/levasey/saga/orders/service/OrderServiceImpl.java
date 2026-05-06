package io.github.levasey.saga.orders.service;

import io.github.levasey.saga.core.dto.Order;
import io.github.levasey.saga.core.event.OrderCreatedEvent;
import io.github.levasey.saga.core.types.OrderStatus;
import io.github.levasey.saga.orders.dao.jpa.entity.OrderEntity;
import io.github.levasey.saga.orders.dao.jpa.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String ordersEventTopicName;

    public OrderServiceImpl(OrderRepository orderRepository,
                            KafkaTemplate<String, Object> kafkaTemplate,
                            @Value("${orders.events.topic.name}") String ordersEventTopicName) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.ordersEventTopicName = ordersEventTopicName;
    }

    @Override
    public Order placeOrder(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setCustomerId(order.getCustomerId());
        entity.setProductId(order.getProductId());
        entity.setProductQuantity(order.getProductQuantity());
        entity.setStatus(OrderStatus.CREATED);
        orderRepository.save(entity);

        OrderCreatedEvent placedOrder = new OrderCreatedEvent(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProductId(),
                entity.getProductQuantity()
        );

        kafkaTemplate.send(ordersEventTopicName, placedOrder);

        return new Order(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProductId(),
                entity.getProductQuantity(),
                entity.getStatus());
    }
}
