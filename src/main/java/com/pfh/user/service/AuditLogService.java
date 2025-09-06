package com.pfh.user.service;

public interface AuditLogService {

    void logLoginSuccess(String userId, String email, String ip, String userAgent);

    void logLoginFailure(String email, String ip, String reason);

    void logAccountLockout(String email, int durationMinutes, String triggerEvent);
}
