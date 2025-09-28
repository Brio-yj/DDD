package com.example.order.web;

import com.example.order.auth.AuthenticationService;
import com.example.order.auth.UserPrincipal;
import com.example.order.domain.CustomerOrder;
import com.example.order.domain.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final AuthenticationService authenticationService;

    public OrderController(OrderService orderService, AuthenticationService authenticationService) {
        this.orderService = orderService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Object productIdValue = body.get("productId");
        Object quantityValue = body.get("quantity");
        Long productId = productIdValue != null ? Long.valueOf(productIdValue.toString()) : null;
        int quantity = quantityValue != null ? Integer.parseInt(quantityValue.toString()) : 0;
        if (productId == null || quantity <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "productId and quantity are required"));
        }
        return authenticationService.authenticate(request)
                .<ResponseEntity<?>>map(user -> doCreateOrder(productId, quantity, user))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "authentication required")));
    }

    private ResponseEntity<?> doCreateOrder(Long productId, int quantity, UserPrincipal user) {
        try {
            CustomerOrder order = orderService.createOrder(productId, quantity, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", order.getId(),
                    "productId", order.getProductId(),
                    "quantity", order.getQuantity(),
                    "status", order.getStatus(),
                    "username", order.getUsername()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/order/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id, HttpServletRequest request) {
        return authenticationService.authenticate(request)
                .map(user -> orderService.findById(id)
                        .<ResponseEntity<?>>map(order -> ResponseEntity.ok(Map.of(
                                "id", order.getId(),
                                "productId", order.getProductId(),
                                "quantity", order.getQuantity(),
                                "status", order.getStatus(),
                                "username", order.getUsername(),
                                "createdAt", order.getCreatedAt())))
                        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "order not found"))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "authentication required")));
    }
}
