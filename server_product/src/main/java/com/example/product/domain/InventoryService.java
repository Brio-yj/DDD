package com.example.product.domain;

import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository productRepository;
    private final InventoryReservationRepository reservationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public InventoryService(ProductRepository productRepository, InventoryReservationRepository reservationRepository) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
    }

    public Optional<Product> findProduct(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public InventoryReservation reserve(Long productId, int quantity) {
        Product product = entityManager.find(Product.class, productId, LockModeType.PESSIMISTIC_WRITE);
        if (product == null) {
            throw new IllegalArgumentException("Product not found");
        }
        if (product.getStock() < quantity) {
            throw new IllegalStateException("Not enough stock");
        }
        product.decreaseStock(quantity);
        InventoryReservation reservation = new InventoryReservation(productId, quantity, "RESERVED");
        reservationRepository.save(reservation);
        log.info("Reserved {} units for product {}", quantity, productId);
        return reservation;
    }
}
