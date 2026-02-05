package com.ecommerce.sellerx.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImpersonationLogRepository extends JpaRepository<ImpersonationLog, Long> {

    List<ImpersonationLog> findByAdminUserIdOrderByCreatedAtDesc(Long adminUserId);

    List<ImpersonationLog> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
}
