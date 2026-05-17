package io.github.levasey.saga.orders.saga;

import io.github.levasey.saga.core.dto.command.ApproveOrderCommand;
import io.github.levasey.saga.core.dto.command.CancelProductReservationCommand;
import io.github.levasey.saga.core.dto.command.ProcessPaymentCommand;
import io.github.levasey.saga.core.dto.command.RejectOrderCommand;
import io.github.levasey.saga.core.dto.command.ReserveProductCommand;
import io.github.levasey.saga.core.dto.event.OrderApprovedEvent;
import io.github.levasey.saga.core.dto.event.OrderCreatedEvent;
import io.github.levasey.saga.core.dto.event.PaymentFailedEvent;
import io.github.levasey.saga.core.dto.event.PaymentProcessedEvent;
import io.github.levasey.saga.core.dto.event.ProductReservationCancelledEvent;
import io.github.levasey.saga.core.dto.event.ProductReservationFailedEvent;
import io.github.levasey.saga.core.dto.event.ProductReservedEvent;
import io.github.levasey.saga.core.types.OrderStatus;
import io.github.levasey.saga.orders.service.OrderHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderSagaTest {

    private static final String PRODUCTS_COMMANDS = "products-commands";
    private static final String PAYMENTS_COMMANDS = "payments-commands";
    private static final String ORDERS_COMMANDS = "orders-commands";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OrderHistoryService orderHistoryService;

    private OrderSaga orderSaga;

    private final UUID orderId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderSaga = new OrderSaga(
                kafkaTemplate,
                PRODUCTS_COMMANDS,
                PAYMENTS_COMMANDS,
                ORDERS_COMMANDS,
                orderHistoryService
        );
    }

    @Test
    void orderCreatedEvent_startsReservation() {
        var event = new OrderCreatedEvent(orderId, customerId, productId, 2);

        orderSaga.handleEvent(event);

        var commandCaptor = ArgumentCaptor.forClass(ReserveProductCommand.class);
        verify(kafkaTemplate).send(eq(PRODUCTS_COMMANDS), eq(orderId.toString()), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(commandCaptor.getValue().getProductId()).isEqualTo(productId);
        assertThat(commandCaptor.getValue().getProductQuantity()).isEqualTo(2);
        verify(orderHistoryService).add(orderId, OrderStatus.CREATED);
    }

    @Test
    void productReservedEvent_startsPayment() {
        var event = new ProductReservedEvent(orderId, productId, BigDecimal.TEN, 2);

        orderSaga.handleEvent(event);

        var commandCaptor = ArgumentCaptor.forClass(ProcessPaymentCommand.class);
        verify(kafkaTemplate).send(eq(PAYMENTS_COMMANDS), eq(orderId.toString()), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void paymentProcessedEvent_approvesOrder() {
        var event = new PaymentProcessedEvent(orderId, UUID.randomUUID());

        orderSaga.handleEvent(event);

        var commandCaptor = ArgumentCaptor.forClass(ApproveOrderCommand.class);
        verify(kafkaTemplate).send(eq(ORDERS_COMMANDS), eq(orderId.toString()), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void paymentFailedEvent_cancelsReservation() {
        var event = new PaymentFailedEvent(orderId, productId, 2);

        orderSaga.handleEvent(event);

        var commandCaptor = ArgumentCaptor.forClass(CancelProductReservationCommand.class);
        verify(kafkaTemplate).send(eq(PRODUCTS_COMMANDS), eq(orderId.toString()), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void productReservationFailedEvent_rejectsOrder() {
        var event = new ProductReservationFailedEvent(productId, orderId, 2);

        orderSaga.handleEvent(event);

        var commandCaptor = ArgumentCaptor.forClass(RejectOrderCommand.class);
        verify(kafkaTemplate).send(eq(ORDERS_COMMANDS), eq(orderId.toString()), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void productReservationCancelledEvent_rejectsOrder() {
        var event = new ProductReservationCancelledEvent(orderId, productId);

        orderSaga.handleEvent(event);

        var commandCaptor = ArgumentCaptor.forClass(RejectOrderCommand.class);
        verify(kafkaTemplate).send(eq(ORDERS_COMMANDS), eq(orderId.toString()), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void orderApprovedEvent_recordsHistory() {
        var event = new OrderApprovedEvent(orderId);

        orderSaga.handleEvent(event);

        verify(orderHistoryService).add(orderId, OrderStatus.APPROVED);
    }
}
