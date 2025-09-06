package com.pfh.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String email;
    private String ipAddress;
    private String userAgent;
    private Instant timestamp;

    private String eventType; // LOGIN_SUCCESS, LOGIN_FAILURE, ACCOUNT_LOCKOUT
    private String failureReason; // invalid_credentials, user_not_found
    private Integer lockoutDurationMinutes;
    private String triggerEvent;

    private String integrityHash; // for tamper-evidence
}
