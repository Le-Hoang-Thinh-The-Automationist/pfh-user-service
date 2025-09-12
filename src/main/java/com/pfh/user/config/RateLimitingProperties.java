package com.pfh.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "rate-limiting")
@Getter
@Setter
public class RateLimitingProperties {
    // Duration (in milliseconds) for which a user account remains locked after exceeding failed login attempts
    private long lockedDurationMs;

    // Time window (in milliseconds) to track failed login attempts
    private long attemptWindowMs;
}