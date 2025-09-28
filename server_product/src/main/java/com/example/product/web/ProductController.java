package com.example.product.web;

import com.example.product.domain.InventoryReservation;
import com.example.product.domain.InventoryService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final InventoryService inventoryService;

    public ProductController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        return inventoryService.findProduct(id)
                .<ResponseEntity<?>>map(product -> ResponseEntity.ok(Map.of(
                        "id", product.getId(),
                        "name", product.getName(),
                        "stock", product.getStock())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "product not found")));
    }

    @PostMapping("/product/{id}/reserve")
    public ResponseEntity<?> reserve(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        int quantity = body.getOrDefault("quantity", 0);
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "quantity must be positive"));
        }
        try {
            InventoryReservation reservation = inventoryService.reserve(id, quantity);
            return ResponseEntity.ok(Map.of(
                    "reservationId", reservation.getId(),
                    "productId", reservation.getProductId(),
                    "quantity", reservation.getQuantity(),
                    "status", reservation.getStatus()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
