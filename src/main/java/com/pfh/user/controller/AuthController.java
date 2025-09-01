package com.pfh.user.controller;

import com.pfh.user.dto.RegistrationRequestDto;
import com.pfh.user.dto.RegistrationResponseDto;
import com.pfh.user.service.AuthService;
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

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDto> register(@Valid @RequestBody RegistrationRequestDto request) {
        RegistrationResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response); 
    }

}
