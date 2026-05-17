package io.github.levasey.saga.payments.service;

import io.github.levasey.saga.core.dto.Payment;
import io.github.levasey.saga.payments.dao.jpa.entity.PaymentEntity;
import io.github.levasey.saga.payments.dao.jpa.repository.PaymentRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final CreditCardProcessorRemoteService ccpRemoteService;
    private final String sampleCreditCardNumber;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              CreditCardProcessorRemoteService ccpRemoteService,
                              @Value("${payment.sample-credit-card-number}") String sampleCreditCardNumber) {
        this.paymentRepository = paymentRepository;
        this.ccpRemoteService = ccpRemoteService;
        this.sampleCreditCardNumber = sampleCreditCardNumber;
    }

    @Override
    @Transactional
    public Payment process(Payment payment) {
        return paymentRepository.findByOrderId(payment.getOrderId())
                .map(this::toPayment)
                .orElseGet(() -> chargeAndPersist(payment));
    }

    private Payment chargeAndPersist(Payment payment) {
        BigDecimal totalPrice = payment.getProductPrice()
                .multiply(new BigDecimal(payment.getProductQuantity()));
        ccpRemoteService.process(new BigInteger(sampleCreditCardNumber), totalPrice);

        PaymentEntity paymentEntity = new PaymentEntity();
        BeanUtils.copyProperties(payment, paymentEntity);
        paymentRepository.save(paymentEntity);

        return toPayment(paymentEntity);
    }

    private Payment toPayment(PaymentEntity paymentEntity) {
        return new Payment(
                paymentEntity.getId(),
                paymentEntity.getOrderId(),
                paymentEntity.getProductId(),
                paymentEntity.getProductPrice(),
                paymentEntity.getProductQuantity()
        );
    }

    @Override
    public List<Payment> findAll() {
        return paymentRepository.findAll().stream()
                .map(this::toPayment)
                .collect(Collectors.toList());
    }
}
