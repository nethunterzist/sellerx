package com.ecommerce.sellerx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableAsync
public class StoreApplication {

    @PostConstruct
    public void init() {
        // Set Turkey timezone for the entire application
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(StoreApplication.class, args);
    }
}
