package com.pfh.user.controller;

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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private static String resolveClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isEmpty()) {
            // X-Forwarded-For can contain a comma-separated list; the first IP is the client
            return header.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }


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

        LoginResponseDto response = authService.login(request, requesterIp, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(response); 
    }

}
