package com.pfh.user.service.impl;

import com.pfh.user.config.AppConstant;
import com.pfh.user.config.LoginRateLimitingProperties;
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
import com.pfh.user.util.RedisUtil;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
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
    private final RedisUtil redisUtil;

    @Autowired
    private LoginRateLimitingProperties loginRateLimitingProperties;

    private final Duration IP_ATTEMPT_WINDOW_DURATION = Duration.ofMillis(loginRateLimitingProperties.getIpAttemptWindowMs());

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
        String userId = user.getId().toString();

        // Record the failed attempt
        redisUtil.recordAuthenUserFailedAttempt(userId, Duration.ofMillis(loginRateLimitingProperties.getAttemptWindowMs()));

        // To lock the account, the failed attempts must exceed the limit AND within the time window 
        if (redisUtil.isAuthenUserRateLimited(userId, AppConstant.MAX_FAILED_LOGIN_ATTEMPTS)) {

            // Set lock time to current time + lock duration
            user.setStatus(UserStatus.LOCKED);
            user.setLockTime(
                timeStampNow.plus(Duration.ofMillis(loginRateLimitingProperties.getLockedDurationMs())) // lock duration
            );

            // Update user's status in DB
            userService.updateUser(user);
            
            // Reset attempts after locking
            redisUtil.resetAuthenUserFailedAttempts(userId);

            return true; // Account is now locked
        }

        return false; // Account is not locked yet
    }

    // Extracted method to validate user credentials
    private UserEntity checkAndGetUserIfExist(LoginRequestDto request, String ip) {
        UserEntity user;

        // Check if the email is registered
        try {
            user = userService.getUserByEmail(request.getEmail());
        } catch (EntityNotFoundException ex) {
            // If not found, record failed attempt for IP in cache for rate limiting
            redisUtil.recordIpFailedAttempt(ip, IP_ATTEMPT_WINDOW_DURATION);
            auditLogService.logLoginFailure(request.getEmail(), ip, "user_not_found");
            throw new CredentialInvalidException("Invalid credentials");
        }
        return user;
    }

    // Extracted method to validate user credentials
    private void checkAndValidateUserCredentials(UserEntity user, LoginRequestDto request, String ip) {
        // Current time in system's default zone
        ZonedDateTime timeStampNow = ZonedDateTime.now(ZoneId.systemDefault());         
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
                    timeStampNow.isAfter(user.getLockTime())
                ) {
                    
                    // Unlock the account
                    user.setStatus(UserStatus.ACTIVE);
                    user.setLockTime(null);

                    userService.updateUser(user);
                } else {
                    redisUtil.recordIpFailedAttempt(ip, IP_ATTEMPT_WINDOW_DURATION);
                    throw new UserStatusException(UserStatus.LOCKED);
                }
                break;
            // If account is not active, throw exception with appropriate message
            default:
                redisUtil.recordIpFailedAttempt(ip, IP_ATTEMPT_WINDOW_DURATION);
                throw new UserStatusException(user.getStatus());
        }

        // Check if the password matches
        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            // If not found, record failed attempt for IP in cache for rate limiting
            redisUtil.recordIpFailedAttempt(ip, IP_ATTEMPT_WINDOW_DURATION);
            auditLogService.logLoginFailure(request.getEmail(), ip, "invalid_credentials");

            // If the account is not locked, proceed to log the failed attempt
            if(!isThisFailedAttemptLockAccount(user)) {
                throw new CredentialInvalidException("Invalid credentials");
            } else {
                auditLogService.logLoginFailure(request.getEmail(), ip, "account_locked_due_to_failed_attempts");
                throw new UserStatusException(UserStatus.LOCKED);
            }
        } else {
            // If login is successful, reset failed attempts for user
            redisUtil.resetAuthenUserFailedAttempts(user.getId().toString());
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
