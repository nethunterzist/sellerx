package com.ecommerce.sellerx.email.repository;

import com.ecommerce.sellerx.email.entity.EmailBaseLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailBaseLayoutRepository extends JpaRepository<EmailBaseLayout, UUID> {

    /**
     * Get the single base layout record.
     * This table should only have one row.
     */
    @Query("SELECT e FROM EmailBaseLayout e ORDER BY e.updatedAt DESC")
    Optional<EmailBaseLayout> findFirst();

    /**
     * Get the base layout (convenience method).
     */
    default EmailBaseLayout getBaseLayout() {
        return findFirst().orElseThrow(() ->
            new IllegalStateException("Email base layout not found in database"));
    }
}
