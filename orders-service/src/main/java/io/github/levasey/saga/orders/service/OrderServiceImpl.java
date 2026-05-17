package io.github.levasey.saga.orders.service;

import io.github.levasey.saga.core.dto.Order;
import io.github.levasey.saga.core.dto.event.OrderApprovedEvent;
import io.github.levasey.saga.core.dto.event.OrderCreatedEvent;
import io.github.levasey.saga.core.types.OrderStatus;
import io.github.levasey.saga.orders.dao.jpa.entity.OrderEntity;
import io.github.levasey.saga.orders.dao.jpa.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import java.util.UUID;

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
    @Transactional
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

        UUID orderId = entity.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send(ordersEventTopicName, orderId.toString(), placedOrder);
            }
        });

        return new Order(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProductId(),
                entity.getProductQuantity(),
                entity.getStatus());
    }

    @Override
    @Transactional
    public void approveOrder(UUID orderId) {
        OrderEntity orderEntity = orderRepository.findById(orderId).orElse(null);
        Assert.notNull(orderEntity, "No such order is found with id " + orderId + " in the database table");
        if (orderEntity.getStatus() == OrderStatus.APPROVED) {
            return;
        }
        orderEntity.setStatus(OrderStatus.APPROVED);
        orderRepository.save(orderEntity);
        OrderApprovedEvent orderApprovedEvent = new OrderApprovedEvent(orderId);
        kafkaTemplate.send(ordersEventTopicName, orderId.toString(), orderApprovedEvent);
    }

    @Override
    @Transactional
    public void rejectOrder(UUID orderId) {
        OrderEntity orderEntity = orderRepository.findById(orderId).orElse(null);
        Assert.notNull(orderEntity, "No such order is found with id " + orderId + " in the database table");
        if (orderEntity.getStatus() == OrderStatus.REJECTED) {
            return;
        }
        orderEntity.setStatus(OrderStatus.REJECTED);
        orderRepository.save(orderEntity);
    }
}
