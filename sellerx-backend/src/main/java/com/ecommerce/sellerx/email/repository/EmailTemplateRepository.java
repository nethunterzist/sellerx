package com.ecommerce.sellerx.email.repository;

import com.ecommerce.sellerx.email.EmailType;
import com.ecommerce.sellerx.email.entity.EmailTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplateEntity, UUID> {

    /**
     * Find template by email type.
     */
    Optional<EmailTemplateEntity> findByEmailType(EmailType emailType);

    /**
     * Find active template by email type.
     */
    Optional<EmailTemplateEntity> findByEmailTypeAndIsActiveTrue(EmailType emailType);

    /**
     * Find all active templates.
     */
    List<EmailTemplateEntity> findByIsActiveTrueOrderByNameAsc();

    /**
     * Check if template exists for type.
     */
    boolean existsByEmailType(EmailType emailType);
}
