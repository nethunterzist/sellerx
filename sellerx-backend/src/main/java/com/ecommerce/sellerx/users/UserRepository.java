package com.ecommerce.sellerx.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByReferralCode(String referralCode);

    boolean existsByReferralCode(String referralCode);

    long countByCreatedAtAfter(LocalDateTime since);

    long countByLastLoginAtAfter(LocalDateTime since);

    Optional<User> findByEmailVerificationToken(String emailVerificationToken);

    // Paginated search by email or name (for admin search)
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchByEmailOrName(@Param("query") String query, Pageable pageable);
}

