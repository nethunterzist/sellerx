package com.ecommerce.sellerx.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "SellerX Backend");
        status.put("environment", "production");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        Map<String, String> info = new HashMap<>();
        info.put("service", "SellerX Backend API");
        info.put("status", "Running");
        info.put("version", "1.0.0");
        return ResponseEntity.ok(info);
    }
}
