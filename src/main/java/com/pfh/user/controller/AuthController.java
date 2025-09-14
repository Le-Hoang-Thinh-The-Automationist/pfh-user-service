package com.pfh.user.controller;

import com.pfh.user.config.AppConstant;
import com.pfh.user.config.LoginRateLimitingProperties;
import com.pfh.user.dto.auth.LoginRequestDto;
import com.pfh.user.dto.auth.LoginResponseDto;
import com.pfh.user.dto.auth.RegistrationRequestDto;
import com.pfh.user.dto.auth.RegistrationResponseDto;
import com.pfh.user.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimitingProperties loginRateLimitingProperties;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static String resolveClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isEmpty()) {
            // X-Forwarded-For can contain a comma-separated list; the first IP is the client
            return header.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ============ REDIS DEPENDENCY ============
    private boolean isRateLimited(String ip) {
        String key = "login:ip:" + ip;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String countStr = ops.get(key);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        return count >= AppConstant.MAX_FAILED_IP_LOGIN_ATTEMPTS;
    }

    private void recordFailedAttempt(String ip) {
        String key = "login:ip:" + ip;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        Long expireMs = loginRateLimitingProperties.getIpAttemptWindowMs();

        // Use Redis atomic increment
        Long count = ops.increment(key);
        // Set expiry only if key is new
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMillis(expireMs));
        }
    }

    private void resetAttempts(String ip) {
        String key = "login:ip:" + ip;
        redisTemplate.delete(key);
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
        if (isRateLimited(requesterIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        LoginResponseDto response = authService.login(request, requesterIp, userAgent);

        // If login failed, record attempt
        if (!response.getMessage().equals("Login successful")) {
            recordFailedAttempt(requesterIp);

        } else {
            // Optionally reset on success
            resetAttempts(requesterIp);
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
