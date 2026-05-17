package io.github.levasey.saga.products.service;

import io.github.levasey.saga.core.dto.Product;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    List<Product> findAll();
    Product reserve(Product desiredProduct, UUID orderId);
    boolean cancelReservation(Product productToCancel, UUID orderId);
    Product save(Product product);
}
