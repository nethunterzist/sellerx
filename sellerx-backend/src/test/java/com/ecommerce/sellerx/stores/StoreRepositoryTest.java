package com.ecommerce.sellerx.stores;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.annotation.Rollback;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class StoreRepositoryTest {
    @Autowired
    private StoreRepository storeRepository;

    @Test
    @Rollback(false)
    public void testSaveStore() {
        Store store = Store.builder()
                .storeName("Test Store")
                .marketplace("trendyol")
                .credentials(new TrendyolCredentials("apiKey", "apiSecret", 123L, null, "Token"))
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        Store saved = storeRepository.save(store);
        assertThat(saved.getId()).isNotNull();
    }
}
