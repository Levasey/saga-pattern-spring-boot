package io.github.levasey.saga.orders.saga;

import io.github.levasey.saga.core.dto.command.ApproveOrderCommand;
import io.github.levasey.saga.core.dto.command.CancelProductReservationCommand;
import io.github.levasey.saga.core.dto.command.ProcessPaymentCommand;
import io.github.levasey.saga.core.dto.command.ReserveProductCommand;
import io.github.levasey.saga.core.dto.event.OrderApprovedEvent;
import io.github.levasey.saga.core.dto.event.OrderCreatedEvent;
import io.github.levasey.saga.core.dto.event.PaymentFailedEvent;
import io.github.levasey.saga.core.dto.event.PaymentProcessedEvent;
import io.github.levasey.saga.core.dto.event.ProductReservationFailedEvent;
import io.github.levasey.saga.core.dto.event.ProductReservedEvent;
import io.github.levasey.saga.core.types.OrderStatus;
import io.github.levasey.saga.orders.service.OrderHistoryService;
import io.github.levasey.saga.orders.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = {
        "${orders.events.topic.name}",
        "${products.events.topic.name}",
        "${payments.events.topic.name}"
})
public class OrderSaga {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String productsCommandsTopicName;
    private final OrderHistoryService orderHistoryService;
    private final OrderService orderService;
    private final String paymentsCommandsTopicName;
    private final String ordersCommandsTopicName;

    public OrderSaga(KafkaTemplate<String, Object> kafkaTemplate,
                     @Value("${products.commands.topic.name}") String productsCommandsTopicName,
                     @Value("${payments.commands.topic.name}") String paymentsCommandsTopicName,
                     @Value("${orders.commands.topic.name}") String ordersCommandsTopicName,
                     OrderHistoryService orderHistoryService,
                     OrderService orderService) {
        this.kafkaTemplate = kafkaTemplate;
        this.productsCommandsTopicName = productsCommandsTopicName;
        this.paymentsCommandsTopicName = paymentsCommandsTopicName;
        this.ordersCommandsTopicName = ordersCommandsTopicName;
        this.orderHistoryService = orderHistoryService;
        this.orderService = orderService;
    }

    @KafkaHandler
    public void handleEvent(@Payload OrderCreatedEvent event) {
        ReserveProductCommand command = new ReserveProductCommand(
                event.getProductId(),
                event.getProductQuantity(),
                event.getOrderId()
        );

        kafkaTemplate.send(productsCommandsTopicName, command);

        orderHistoryService.add(event.getOrderId(), OrderStatus.CREATED);
    }

    @KafkaHandler
    public void handleEvent(@Payload ProductReservedEvent event) {
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                event.getOrderId(),
                event.getProductId(),
                event.getProductPrice(),
                event.getProductQuantity()
        );

        kafkaTemplate.send(paymentsCommandsTopicName, command);
    }

    @KafkaHandler
    public void handleEvent(@Payload ProductReservationFailedEvent event) {
        orderService.rejectOrder(event.getOrderId());
        orderHistoryService.add(event.getOrderId(), OrderStatus.REJECTED);
    }

    @KafkaHandler
    public void handleEvent(@Payload PaymentProcessedEvent event) {
        ApproveOrderCommand approveOrderCommand = new ApproveOrderCommand(event.getOrderId());
        kafkaTemplate.send(ordersCommandsTopicName, approveOrderCommand);
    }

    @KafkaHandler
    public void handleEvent(@Payload PaymentFailedEvent event) {
        CancelProductReservationCommand rollback = new CancelProductReservationCommand(
                event.getProductId(),
                event.getProductQuantity(),
                event.getOrderId());
        kafkaTemplate.send(productsCommandsTopicName, rollback);

        orderService.rejectOrder(event.getOrderId());
        orderHistoryService.add(event.getOrderId(), OrderStatus.REJECTED);
    }

    @KafkaHandler
    public void handleEvent(@Payload OrderApprovedEvent event) {
        orderHistoryService.add(event.getOrderId(), OrderStatus.APPROVED);
    }
}
