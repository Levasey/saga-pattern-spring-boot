package io.github.levasey.saga.products.dao.jpa.repository;

import io.github.levasey.saga.products.dao.jpa.entity.ProductReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductReservationRepository extends JpaRepository<ProductReservationEntity, UUID> {
}
