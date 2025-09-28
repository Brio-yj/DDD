package com.example.product.config;

import com.example.product.domain.Product;
import com.example.product.domain.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedProducts(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Product("Keyboard", 100));
                repository.save(new Product("Mouse", 150));
                repository.save(new Product("Monitor", 40));
                log.info("Seeded default products");
            }
        };
    }
}
