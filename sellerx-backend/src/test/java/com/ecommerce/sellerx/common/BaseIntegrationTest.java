package com.ecommerce.sellerx.common;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests.
 * Provides a shared PostgreSQL TestContainer for all integration tests.
 *
 * Usage:
 * <pre>
 * class MyIntegrationTest extends BaseIntegrationTest {
 *     @Autowired
 *     private MyService myService;
 *
 *     @Test
 *     void testSomething() {
 *         // Test with real database
 *     }
 * }
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {

    /**
     * Shared PostgreSQL container for all integration tests.
     * Uses @ServiceConnection for automatic Spring Boot configuration.
     */
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
            .withStartupTimeout(java.time.Duration.ofMinutes(2));
}
