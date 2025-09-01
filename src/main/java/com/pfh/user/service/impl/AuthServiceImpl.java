package com.pfh.user.service.impl;

import com.pfh.user.dto.RegistrationRequestDto;
import com.pfh.user.dto.RegistrationResponseDto;
import com.pfh.user.exception.PasswordMismatchException;
import com.pfh.user.service.AuthService;
import com.pfh.user.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;

    // Dummy encoder using Argon2id with OWASP-aligned parameters
    private final Argon2PasswordEncoder encoder =
            new Argon2PasswordEncoder(16, 32, 2, 1 << 16, 3);

    @Override
    public RegistrationResponseDto register(RegistrationRequestDto request) {
        // Check if the password and the confirmed password matches
        if (!request.getPassword().equals(request.getConfirmPassword())){
            throw new PasswordMismatchException();
        }
        
        // Encrypting password before passing it to the UserService
        request.setPassword(encoder.encode(request.getPassword()));

        return userService.createUser(request);
    }    
}
