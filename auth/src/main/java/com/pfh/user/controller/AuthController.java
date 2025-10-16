package com.pfh.user.controller;

import com.pfh.user.config.AppConstant;
import com.pfh.user.dto.auth.LoginRequestDto;
import com.pfh.user.dto.auth.LoginResponseDto;
import com.pfh.user.dto.auth.RegistrationRequestDto;
import com.pfh.user.dto.auth.RegistrationResponseDto;
import com.pfh.user.exception.RateLimitExceededException;
import com.pfh.user.service.AuthService;
import com.pfh.user.service.RedisService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Autowired  
    private final RedisService redisService;

    private static String resolveClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isEmpty()) {
            // X-Forwarded-For can contain a comma-separated list; the first IP is the client
            return header.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ============ ENDPOINTS ============
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDto> register(@Valid @RequestBody RegistrationRequestDto request) {
        RegistrationResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response); 
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
        @Valid @RequestBody LoginRequestDto request,
        HttpServletRequest httpRequest
    ) {
        String requesterIp = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Rate limit check
        if (redisService.isRateLimited(
                AppConstant.REDIS_KEY_PREFIX_FAILED_ATTEMPT_IP, 
                requesterIp, 
                AppConstant.MAX_FAILED_IP_LOGIN_ATTEMPTS
        )) {
            throw new RateLimitExceededException("Too many failed login attempts from this IP. Please try slow down.");
        }

        LoginResponseDto response = authService.login(request, requesterIp, userAgent);

        // If login failed, record attempt
        if (response.getMessage().equals("Login successful")) {
            redisService.resetAttempts(AppConstant.REDIS_KEY_PREFIX_FAILED_ATTEMPT_IP, requesterIp);
        } 

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
