package com.ecommerce.sellerx.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByReferralCode(String referralCode);

    boolean existsByReferralCode(String referralCode);

    long countByCreatedAtAfter(LocalDateTime since);

    long countByLastLoginAtAfter(LocalDateTime since);
}

