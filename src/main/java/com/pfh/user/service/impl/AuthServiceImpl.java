package com.pfh.user.service.impl;

import com.pfh.user.config.AppConstant;
import com.pfh.user.config.RateLimitingProperties;
import com.pfh.user.dto.auth.LoginRequestDto;
import com.pfh.user.dto.auth.LoginResponseDto;
import com.pfh.user.dto.auth.RegistrationRequestDto;
import com.pfh.user.dto.auth.RegistrationResponseDto;
import com.pfh.user.entity.UserEntity;
import com.pfh.user.exception.CredentialInvalidException;
import com.pfh.user.exception.PasswordIsWeakException;
import com.pfh.user.exception.PasswordMismatchException;
import com.pfh.user.enums.UserStatus;
import com.pfh.user.exception.UserStatusException;
import com.pfh.user.service.AuditLogService;
import com.pfh.user.service.AuthService;
import com.pfh.user.service.UserService;
import com.pfh.user.util.JwtUtil;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final AuditLogService auditLogService;

    // Use Argon2 for password hashing with OWASP recommended parameters
    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(
        AppConstant.ARGON2_SALT_LENGTH,
        AppConstant.ARGON2_HASH_LENGTH,
        AppConstant.ARGON2_PARALLELISM,
        AppConstant.ARGON2_MEMORY,
        AppConstant.ARGON2_ITERATIONS
    );

    private final JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RateLimitingProperties rateLimitingProperties;

    // Helper class for storing attempt info
    public static class FailedAttemptInfo implements Serializable {
        private ZonedDateTime firstAttemptWindowTimestamp;
        private int attempts;

        public FailedAttemptInfo(ZonedDateTime firstAttemptWindowTimestamp, int attempts) {
            this.firstAttemptWindowTimestamp = firstAttemptWindowTimestamp;
            this.attempts = attempts;
        }

        public ZonedDateTime getFirstAttemptWindowTimestamp() { return firstAttemptWindowTimestamp; }
        public int getAttempts() { return attempts; }
        public void setAttempts(int attempts) { this.attempts = attempts; }
    }

// ============ REGISTER ============
    // Check password strength
    private static void checkPasswordStrength(String inputPassword){
        // Check if password is in common list
        if (AppConstant.COMMON_PASSWORDS.contains(inputPassword.toLowerCase())) {
            throw new PasswordIsWeakException("Password is too common");
        }

        // Check length
        if (inputPassword.length() <= AppConstant.MINIMUM_PASSWORD_LENGTH) {
            throw new PasswordIsWeakException("Password must be at least 12 characters long");
        }

        // Check for uppercase, lowercase, digit, and special character
        if (!inputPassword.matches(".*[A-Z].*") ||
            !inputPassword.matches(".*[a-z].*") ||
            !inputPassword.matches(".*\\d.*") ||
            !inputPassword.matches(".*[!@#$%^&*()].*")) {
            throw new PasswordIsWeakException("Password must contain at least one uppercase, lowercase, digit, and special character");
        }
        
    }

    // Register method
    @Override
    public RegistrationResponseDto register(RegistrationRequestDto request) {
        // Check for password if it is strong enough
        checkPasswordStrength(request.getPassword());
        
        // Check if the password and the confirmed password matches
        if (!request.getPassword().equals(request.getConfirmPassword())){
            throw new PasswordMismatchException();
        }
        
        // Encrypting password before passing it to the UserService
        request.setPassword(encoder.encode(request.getPassword()));

        return userService.createUser(request);
    }    


// ============ LOGIN ============

    // Extracted method to handle failed login attempts and lock account if necessary
    private boolean isThisFailedAttemptLockAccount(UserEntity user) {
        // Current time in system's default zone
        ZonedDateTime timeStampNow = ZonedDateTime.now(ZoneId.systemDefault()); 

        // Redis key for storing failed attempts
        String key = "login:fail:" + user.getId();
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        // Get current attempt info from cache
        FailedAttemptInfo attemptInfo = (FailedAttemptInfo) ops.get(key);

        if (attemptInfo == null) {
            // First failed attempt: create new info
            attemptInfo = new FailedAttemptInfo(
                // Set first attempt window timestamp from now
                timeStampNow.plus(Duration.ofMillis(rateLimitingProperties.getAttemptWindowMs())), 1);
        } else {
            // Increase failed attempts
            attemptInfo.setAttempts(attemptInfo.getAttempts() + 1);
        }

        // Store/update in Redis with 15 min expiry
        ops.set(key, attemptInfo, Duration.ofMillis(rateLimitingProperties.getAttemptWindowMs()));

        // To lock the account, the failed attempts must exceed the limit AND within the time window 
        if (
            (timeStampNow.isBefore(attemptInfo.getFirstAttemptWindowTimestamp())) &&
            (attemptInfo.getAttempts() >= AppConstant.MAX_FAILED_LOGIN_ATTEMPTS)
        ) {

            // Set lock time to current time + lock duration
            user.setStatus(UserStatus.LOCKED);
            user.setLockTime(
                timeStampNow.plus(Duration.ofMillis(rateLimitingProperties.getLockedDurationMs())) // lock duration
            );

            // Update user's status in DB
            userService.updateUser(user);

            return true; // Account is now locked
        }

        return false; // Account is not locked yet
    }

    // Extracted method to validate user credentials
    private UserEntity checkAndGetUserIfExist(LoginRequestDto request, String ip) {
        // 1. Check if the email is registered
        UserEntity user;

        // Check if the email is registered
        try {
            user = userService.getUserByEmail(request.getEmail());
        } catch (EntityNotFoundException ex) {
            auditLogService.logLoginFailure(request.getEmail(), ip, "user_not_found");
            throw new CredentialInvalidException("Invalid credentials");
        }
        return user;
    }

    // Extracted method to validate user credentials
    private void checkAndValidateUserCredentials(UserEntity user, LoginRequestDto request, String ip) {
        // Check current user status (Active, Locked, Inactive, etc.)
        switch (user.getStatus()) {
            case UserStatus.ACTIVE:
                break;
            
            // If the account is locked, check if the lock duration has passed
            case UserStatus.LOCKED:
                if(
                    // Lock time is set 
                    user.getLockTime() != null &&
                    // Current time is after lock time
                   ZonedDateTime.now(ZoneId.systemDefault()).isAfter(user.getLockTime())
                ) {
                    
                    // Unlock the account
                    user.setStatus(UserStatus.ACTIVE);
                    user.setLockTime(null);

                    userService.updateUser(user);
                } else {
                    throw new UserStatusException(UserStatus.LOCKED);
                }
                
            // If account is not active, throw exception with appropriate message
            default:
                throw new UserStatusException(user.getStatus());
        }

        // Check if the password matches
        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditLogService.logLoginFailure(request.getEmail(), ip, "invalid_credentials");

            // If the account is not locked, proceed to log the failed attempt
            if(!isThisFailedAttemptLockAccount(user)) {
                throw new CredentialInvalidException("Invalid credentials");
            } else {
                auditLogService.logLoginFailure(request.getEmail(), ip, "account_locked_due_to_failed_attempts");
                throw new UserStatusException(UserStatus.LOCKED);
            }
        }
    }

    // Login method
    @Override
    public LoginResponseDto login(LoginRequestDto request, String ip, String userAgent) {
        UserEntity user;

        // Check if the email is registered
        user = checkAndGetUserIfExist(request, ip);

        // Validate credentials and check status
        checkAndValidateUserCredentials(user, request, ip);

        auditLogService.logLoginSuccess(
            String.valueOf(user.getId()),
            user.getEmail(),
            ip,
            userAgent
        );

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("roles", new String[]{user.getRole().toString()});


        return LoginResponseDto.builder()
            .token(jwtUtil.generateToken(
                String.valueOf(user.getId()),
                claims
            ))
            .message("Login successful")
            .claims(claims)
            .build();
    }
}
