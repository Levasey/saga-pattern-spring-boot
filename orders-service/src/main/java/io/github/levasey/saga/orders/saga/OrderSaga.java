package io.github.levasey.saga.orders.saga;

import io.github.levasey.saga.core.dto.command.*;
import io.github.levasey.saga.core.dto.event.*;
import io.github.levasey.saga.core.types.OrderStatus;
import io.github.levasey.saga.orders.service.OrderHistoryService;
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
    private final String paymentsCommandsTopicName;
    private final String ordersCommandsTopicName;

    public OrderSaga(KafkaTemplate<String, Object> kafkaTemplate,
                     @Value("${products.commands.topic.name}") String productsCommandsTopicName,
                     @Value("${payments.commands.topic.name}") String paymentsCommandsTopicName,
                     @Value("${orders.commands.topic.name}") String ordersCommandsTopicName,
                     OrderHistoryService orderHistoryService) {
        this.kafkaTemplate = kafkaTemplate;
        this.productsCommandsTopicName = productsCommandsTopicName;
        this.paymentsCommandsTopicName = paymentsCommandsTopicName;
        this.ordersCommandsTopicName = ordersCommandsTopicName;
        this.orderHistoryService = orderHistoryService;
    }

    @KafkaHandler
    public void handleEvent(@Payload OrderCreatedEvent event) {
        ReserveProductCommand command = new ReserveProductCommand(
                event.getProductId(),
                event.getProductQuantity(),
                event.getOrderId()
        );

        kafkaTemplate.send(productsCommandsTopicName, event.getOrderId().toString(), command);

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

        kafkaTemplate.send(paymentsCommandsTopicName, event.getOrderId().toString(), command);
    }

    @KafkaHandler
    public void handleEvent(@Payload PaymentProcessedEvent event) {
        ApproveOrderCommand approveOrderCommand = new ApproveOrderCommand(event.getOrderId());
        kafkaTemplate.send(ordersCommandsTopicName, event.getOrderId().toString(), approveOrderCommand);
    }

    @KafkaHandler
    public void handleEvent(@Payload PaymentFailedEvent event) {
        CancelProductReservationCommand rollback = new CancelProductReservationCommand(
                event.getProductId(),
                event.getProductQuantity(),
                event.getOrderId());
        kafkaTemplate.send(productsCommandsTopicName, event.getOrderId().toString(), rollback);
    }

    @KafkaHandler
    public void handleEvent(@Payload ProductReservationFailedEvent event) {
        RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(event.getOrderId());
        kafkaTemplate.send(ordersCommandsTopicName, event.getOrderId().toString(), rejectOrderCommand);
    }

    @KafkaHandler
    public void handleEvent(@Payload OrderApprovedEvent event) {
        orderHistoryService.add(event.getOrderId(), OrderStatus.APPROVED);
    }

    @KafkaHandler
    public void handleEvent(@Payload ProductReservationCancelledEvent event) {
        RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(event.getOrderId());
        kafkaTemplate.send(ordersCommandsTopicName, event.getOrderId().toString(), rejectOrderCommand);
    }
}
