package com.example.order.domain;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProductClient(RestTemplate restTemplate, @Value("${services.product.base-url:http://localhost:8084}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void reserve(Long productId, int quantity) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(Map.of("quantity", quantity), headers);
        String url = baseUrl + "/product/" + productId + "/reserve";
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {});
            if (response.getStatusCode().is5xxServerError()) {
                throw new IllegalStateException("Inventory service error");
            }
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Failed to reserve inventory: " + response.getStatusCode());
            }
        } catch (RestClientException ex) {
            log.error("Failed to reserve product {}", productId, ex);
            throw new IllegalStateException("Inventory reservation failed");
        }
    }
}
