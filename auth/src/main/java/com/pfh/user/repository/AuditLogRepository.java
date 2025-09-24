package com.pfh.user.repository;

import com.pfh.user.entity.AuditLogEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    AuditLogEntity findTopByOrderByIdDesc();
}
