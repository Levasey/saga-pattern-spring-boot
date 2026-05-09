package io.github.levasey.saga.orders.service.handler;

import io.github.levasey.saga.core.dto.command.ApproveOrderCommand;
import io.github.levasey.saga.core.dto.command.RejectOrderCommand;
import io.github.levasey.saga.orders.service.OrderService;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = "${orders.commands.topic.name}")
public class OrdersCommandsHandler {
    private final OrderService orderService;

    public OrdersCommandsHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaHandler
    public void handleCommand(@Payload ApproveOrderCommand approveOrderCommand) {
        orderService.approvedOrder(approveOrderCommand.getOrderId());
    }

    @KafkaHandler
    public void handleCommand(@Payload RejectOrderCommand rejectOrderCommand) {
        orderService.rejectOrder(rejectOrderCommand.getOrderId());
    }
}
