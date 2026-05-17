package io.github.levasey.saga.payments.dao.jpa.repository;

import io.github.levasey.saga.payments.dao.jpa.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByOrderId(UUID orderId);
}
