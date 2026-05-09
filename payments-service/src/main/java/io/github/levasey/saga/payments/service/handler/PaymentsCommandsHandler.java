package io.github.levasey.saga.payments.service.handler;

import io.github.levasey.saga.core.dto.Payment;
import io.github.levasey.saga.core.dto.command.ProcessPaymentCommand;
import io.github.levasey.saga.core.dto.event.PaymentFailedEvent;
import io.github.levasey.saga.core.dto.event.PaymentProcessedEvent;
import io.github.levasey.saga.core.exceptions.CreditCardProcessorUnavailableException;
import io.github.levasey.saga.payments.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = "${payments.commands.topic.name}")
public class PaymentsCommandsHandler {

    private final PaymentService service;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String paymentEventsTopicName;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public PaymentsCommandsHandler(PaymentService service,
                                   KafkaTemplate<String, Object> kafkaTemplate,
                                   @Value("${payments.events.topic.name}") String paymentEventsTopicName) {
        this.service = service;
        this.kafkaTemplate = kafkaTemplate;
        this.paymentEventsTopicName = paymentEventsTopicName;
    }

    @KafkaHandler
    public void handleCommand(@Payload ProcessPaymentCommand command) {
        try {
            Payment payment = new Payment(command.getOrderId(),
                    command.getProductId(),
                    command.getProductPrice(),
                    command.getProductQuantity());

            Payment processedPayment = service.process(payment);

            PaymentProcessedEvent paymentProcessedEvent = new PaymentProcessedEvent(processedPayment.getOrderId(),
                    processedPayment.getId());
            kafkaTemplate.send(paymentEventsTopicName, paymentProcessedEvent);
        } catch (CreditCardProcessorUnavailableException e) {
            logger.error(e.getLocalizedMessage(), e);
            PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent(command.getOrderId(),
                    command.getProductId(),
                    command.getProductQuantity());
            kafkaTemplate.send(paymentEventsTopicName, paymentFailedEvent);
        }
    }
}
