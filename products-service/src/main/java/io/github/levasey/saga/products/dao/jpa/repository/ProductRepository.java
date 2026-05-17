package io.github.levasey.saga.products.dao.jpa.repository;

import io.github.levasey.saga.products.dao.jpa.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductEntity p SET p.quantity = p.quantity - :quantity " +
            "WHERE p.id = :productId AND p.quantity >= :quantity")
    int decreaseQuantityIfAvailable(@Param("productId") UUID productId, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductEntity p SET p.quantity = p.quantity + :quantity WHERE p.id = :productId")
    int increaseQuantity(@Param("productId") UUID productId, @Param("quantity") int quantity);
}
