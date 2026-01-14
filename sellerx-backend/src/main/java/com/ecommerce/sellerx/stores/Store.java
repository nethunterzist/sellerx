package com.ecommerce.sellerx.stores;

import jakarta.persistence.*;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import lombok.*;
import java.time.LocalDateTime;
import com.ecommerce.sellerx.users.User;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "stores")
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "marketplace", nullable = false)
    private String marketplace;

    @Type(JsonBinaryType.class)
    @Column(name = "credentials", columnDefinition = "jsonb", nullable = false)
    private MarketplaceCredentials credentials;
    
    @Column(name = "webhook_id")
    private String webhookId; // Trendyol webhook ID for this store

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
