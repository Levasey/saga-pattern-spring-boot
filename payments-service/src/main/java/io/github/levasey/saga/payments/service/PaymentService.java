package io.github.levasey.saga.payments.service;

import io.github.levasey.saga.core.dto.Payment;

import java.util.List;

public interface PaymentService {
    List<Payment> findAll();

    Payment process(Payment payment);
}
