package com.pfh.user.service.impl;

import com.pfh.user.config.AppConstant;
import com.pfh.user.dto.auth.LoginRequestDto;
import com.pfh.user.dto.auth.LoginResponseDto;
import com.pfh.user.dto.auth.RegistrationRequestDto;
import com.pfh.user.dto.auth.RegistrationResponseDto;
import com.pfh.user.entity.UserEntity;
import com.pfh.user.exception.CredentialInvalidException;
import com.pfh.user.exception.PasswordIsWeakException;
import com.pfh.user.exception.PasswordMismatchException;
import com.pfh.user.service.AuditLogService;
import com.pfh.user.service.AuthService;
import com.pfh.user.service.UserService;
import com.pfh.user.util.JwtUtil;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
            throw new CredentialInvalidException("Invalid credentials");
        }

        // Check if the password matches
        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditLogService.logLoginFailure(request.getEmail(), ip, "invalid_credentials");
            throw new CredentialInvalidException("Invalid credentials");
        }

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
            .claims(claims)
            .build();
    }
}
