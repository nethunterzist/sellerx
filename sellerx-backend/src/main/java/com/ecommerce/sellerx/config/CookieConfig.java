package com.ecommerce.sellerx.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.cookie")
public class CookieConfig {
    private boolean secure = false;
    private boolean httpOnly = true;
}
