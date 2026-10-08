package com.glowvera.web;

import com.glowvera.repository.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final ProductRepository products;
    private final Clock clock;

    public HealthController(ProductRepository products, Clock clock) {
        this.products = products;
        this.clock = clock;
    }

    @GetMapping
    public Map<String, Object> health() {
        return Map.of("status", "ok", "time", Instant.now(clock).toString());
    }

    @GetMapping("/db")
    public Map<String, Object> db() {
        return Map.of("status", "ok", "products", products.count());
    }
}
