package com.pfh.user.service.impl;

import com.pfh.user.entity.AuditLogEntity;
import com.pfh.user.repository.AuditLogRepository;
import com.pfh.user.service.AuditLogService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository repository;

    @Override
    public void logLoginSuccess(String userId, String email, String ip, String userAgent) {
        AuditLogEntity entry = AuditLogEntity.builder()
                .userId(userId)
                .email(email)
                .ipAddress(ip)
                .userAgent(userAgent)
                .timestamp(Instant.now())
                .eventType("LOGIN_SUCCESS")
                .integrityHash(generateIntegrityHash(email + "LOGIN_SUCCESS"))
                .build();
        repository.save(entry);
    }

    @Override
    public void logLoginFailure(String email, String ip, String reason) {
        AuditLogEntity entry = AuditLogEntity.builder()
                .email(email)
                .ipAddress(ip)
                .timestamp(Instant.now())
                .eventType("LOGIN_FAILURE")
                .failureReason(reason)
                .integrityHash(generateIntegrityHash(email + reason))
                .build();
        repository.save(entry);
    }

    @Override
    public void logAccountLockout(String email, int durationMinutes, String triggerEvent) {
        AuditLogEntity entry = AuditLogEntity.builder()
                .email(email)
                .timestamp(Instant.now())
                .eventType("ACCOUNT_LOCKOUT")
                .lockoutDurationMinutes(durationMinutes)
                .triggerEvent(triggerEvent)
                .integrityHash(generateIntegrityHash(email + "LOCKOUT"))
                .build();
        repository.save(entry);
    }

    private String generateIntegrityHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "hash-error";
        }
    }
}
