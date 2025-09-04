package com.pfh.user.service.impl;

import com.pfh.user.dto.auth.LoginRequestDto;
import com.pfh.user.dto.auth.LoginResponseDto;
import com.pfh.user.dto.auth.RegistrationRequestDto;
import com.pfh.user.dto.auth.RegistrationResponseDto;
import com.pfh.user.entity.UserEntity;
import com.pfh.user.exception.CredentialInValidException;
import com.pfh.user.exception.PasswordIsWeakException;
import com.pfh.user.exception.PasswordMismatchException;
import com.pfh.user.service.AuditLogService;
import com.pfh.user.service.AuthService;
import com.pfh.user.service.UserService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final AuditLogService auditLogService;

    // Dummy encoder using Argon2id with OWASP-aligned parameters
    private final Argon2PasswordEncoder encoder =
            new Argon2PasswordEncoder(16, 32, 2, 1 << 16, 3);


    // Configuration macro
    private static final List<String> COMMON_PASSWORDS = Arrays.asList(
        "password1234",        // 12 chars: predictable word + digits
        "iloveyou2020!!",      // 14 chars: common phrase + popular year + symbols
        "welcome12345!",       // 14 chars: greeting + digits + symbol
        "qwertyuiop123",       // 13 chars: extended keyboard sequence + digits
        "abc123abc123"        // 12 chars: repeated basic pattern
    );

    private static final int MINIMUM_PASSWORD_LENGTH=12;

    private static void checkPasswordStrength(String inputPassword){
        // Check if password is in common list
        if (COMMON_PASSWORDS.contains(inputPassword.toLowerCase())) {
            throw new PasswordIsWeakException("Password is too common");
        }

        // Check length
        if (inputPassword.length() <= MINIMUM_PASSWORD_LENGTH) {
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


    @Override
    public LoginResponseDto login(LoginRequestDto request, String ip, String userAgent) {
        UserEntity user;
        
        // Check if the email is registered
        try {
            user = userService.getUserByEmail(request.getEmail());
        } catch (EntityNotFoundException ex) {
            auditLogService.logLoginFailure(request.getEmail(), ip, "user_not_found");
            throw new CredentialInValidException("Invalid credentials");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditLogService.logLoginFailure(request.getEmail(), ip, "invalid_credentials");
            throw new CredentialInValidException("Invalid credentials");
        }

        auditLogService.logLoginSuccess(
            String.valueOf(user.getId()),
            user.getEmail(),
            ip,
            userAgent
        );

        return LoginResponseDto.builder()
                .token("dummy-jwt-token") // Replace with actual JWT generation
                .claims(Map.of(
                        "userId", String.valueOf(user.getId()),
                        "email", user.getEmail(),
                        "roles", new String[]{user.getRole().toString()}, // assuming getRole() returns "USER", etc.
                        "exp", System.currentTimeMillis() / 1000 + 900
                ))
                .build();
    }
}
