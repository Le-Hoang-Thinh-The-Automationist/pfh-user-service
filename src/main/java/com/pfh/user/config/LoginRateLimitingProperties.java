package com.pfh.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "login-rate-limiting")
@Getter
@Setter
public class LoginRateLimitingProperties {
    // Duration (in milliseconds) for which a user account remains locked after exceeding failed login attempts
    private long lockedDurationMs;

    // Time window (in milliseconds) to track failed login attempts
    private long attemptWindowMs;

    // Time window (in milliseconds) to track failed login attempts from the same IP address
    private long ipAttemptWindowMs;
}