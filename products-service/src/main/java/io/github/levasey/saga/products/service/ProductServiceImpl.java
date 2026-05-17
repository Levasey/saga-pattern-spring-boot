package io.github.levasey.saga.products.service;

import io.github.levasey.saga.core.dto.Product;
import io.github.levasey.saga.core.exceptions.ProductInsufficientQuantityException;
import io.github.levasey.saga.products.dao.jpa.entity.ProductEntity;
import io.github.levasey.saga.products.dao.jpa.entity.ProductReservationEntity;
import io.github.levasey.saga.products.dao.jpa.repository.ProductRepository;
import io.github.levasey.saga.products.dao.jpa.repository.ProductReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductReservationRepository productReservationRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              ProductReservationRepository productReservationRepository) {
        this.productRepository = productRepository;
        this.productReservationRepository = productReservationRepository;
    }

    @Override
    @Transactional
    public Product reserve(Product desiredProduct, UUID orderId) {
        var existingReservation = productReservationRepository.findById(orderId);
        if (existingReservation.isPresent()) {
            ProductReservationEntity reservation = existingReservation.get();
            if (!reservation.isCancelled()) {
                return toReservedProduct(reservation);
            }
        }

        ProductEntity productEntity = productRepository.findById(desiredProduct.getId())
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + desiredProduct.getId()));

        int updatedRows = productRepository.decreaseQuantityIfAvailable(
                desiredProduct.getId(),
                desiredProduct.getQuantity()
        );
        if (updatedRows == 0) {
            throw new ProductInsufficientQuantityException(productEntity.getId(), orderId);
        }

        ProductReservationEntity reservation = new ProductReservationEntity();
        reservation.setOrderId(orderId);
        reservation.setProductId(desiredProduct.getId());
        reservation.setQuantity(desiredProduct.getQuantity());
        reservation.setPrice(productEntity.getPrice());
        reservation.setCancelled(false);
        productReservationRepository.save(reservation);

        return new Product(
                desiredProduct.getId(),
                productEntity.getName(),
                productEntity.getPrice(),
                desiredProduct.getQuantity()
        );
    }

    @Override
    @Transactional
    public boolean cancelReservation(Product productToCancel, UUID orderId) {
        var reservationOptional = productReservationRepository.findById(orderId);
        if (reservationOptional.isEmpty() || reservationOptional.get().isCancelled()) {
            return false;
        }

        ProductReservationEntity reservation = reservationOptional.get();
        productRepository.increaseQuantity(reservation.getProductId(), reservation.getQuantity());
        reservation.setCancelled(true);
        productReservationRepository.save(reservation);
        return true;
    }

    @Override
    public Product save(Product product) {
        ProductEntity productEntity = new ProductEntity();
        productEntity.setName(product.getName());
        productEntity.setPrice(product.getPrice());
        productEntity.setQuantity(product.getQuantity());
        productRepository.save(productEntity);

        return new Product(productEntity.getId(), product.getName(), product.getPrice(), product.getQuantity());
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll().stream()
                .map(entity -> new Product(entity.getId(), entity.getName(), entity.getPrice(), entity.getQuantity()))
                .collect(Collectors.toList());
    }

    private Product toReservedProduct(ProductReservationEntity reservation) {
        ProductEntity productEntity = productRepository.findById(reservation.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + reservation.getProductId()));
        return new Product(
                reservation.getProductId(),
                productEntity.getName(),
                reservation.getPrice(),
                reservation.getQuantity()
        );
    }
}
