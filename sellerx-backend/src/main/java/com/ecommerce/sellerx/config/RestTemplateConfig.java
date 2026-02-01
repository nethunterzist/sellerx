package com.ecommerce.sellerx.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate with Apache HttpClient 5 connection pooling.
 * Optimized for Trendyol API calls with rate limiting (10 req/sec).
 *
 * Pool settings:
 * - maxConnTotal: 50 (total connections across all routes)
 * - maxConnPerRoute: 20 (connections per host, mainly api.trendyol.com)
 * - connectTimeout: 5s
 * - connectionRequestTimeout: 5s (wait for connection from pool)
 * - responseTimeout: 30s (for long settlement/sync queries)
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Connection-level timeouts
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .setSocketTimeout(Timeout.ofSeconds(30))
                .build();

        // Connection pool
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(50);
        connManager.setDefaultMaxPerRoute(20);
        connManager.setDefaultConnectionConfig(connectionConfig);

        // Request-level timeout (waiting for connection from pool)
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(30))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return builder
                .requestFactory(() -> factory)
                .build();
    }
}
