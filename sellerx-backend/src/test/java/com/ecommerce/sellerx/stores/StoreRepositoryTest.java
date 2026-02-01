package com.ecommerce.sellerx.stores;

import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import com.ecommerce.sellerx.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.Rollback;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class StoreRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Rollback
    public void testSaveStore() {
        // First create a user
        User user = User.builder()
                .name("Test User")
                .email("test-" + UUID.randomUUID() + "@test.com")
                .password("password123")
                .role(Role.USER)
                .build();
        User savedUser = userRepository.save(user);

        // Then create a store with the user
        Store store = Store.builder()
                .storeName("Test Store")
                .marketplace("trendyol")
                .credentials(new TrendyolCredentials("apiKey", "apiSecret", 123L, null, "Token"))
                .user(savedUser)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        Store saved = storeRepository.save(store);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser()).isNotNull();
    }
}
