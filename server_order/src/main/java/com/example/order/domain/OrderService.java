package com.example.order.domain;

import com.example.order.auth.UserPrincipal;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(CustomerOrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    public CustomerOrder createOrder(Long productId, int quantity, UserPrincipal user) {
        productClient.reserve(productId, quantity);
        CustomerOrder order = new CustomerOrder(productId, quantity, "CREATED", user.username());
        return orderRepository.save(order);
    }

    public Optional<CustomerOrder> findById(Long id) {
        return orderRepository.findById(id);
    }
}
