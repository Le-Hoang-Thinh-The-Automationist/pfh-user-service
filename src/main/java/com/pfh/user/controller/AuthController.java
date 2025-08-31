package com.pfh.user.controller;

import com.pfh.user.dto.RegistrationRequestDto;
import com.pfh.user.dto.RegistrationResponseDto;
import com.pfh.user.exception.DuplicateEmailException;
import com.pfh.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDto> register(@Valid @RequestBody RegistrationRequestDto request) {
        RegistrationResponseDto response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response); 
    }

}
