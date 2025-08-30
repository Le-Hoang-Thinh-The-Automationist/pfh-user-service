package com.pfh.user.service;

import com.pfh.user.dto.RegistrationRequestDto;
import com.pfh.user.dto.RegistrationResponseDto;
import com.pfh.user.entity.UserEntity;
import com.pfh.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.pfh.user.exception.DuplicateEmailException;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    // Dummy encoder using Argon2id with OWASP-aligned parameters
    private final Argon2PasswordEncoder encoder =
            new Argon2PasswordEncoder(16, 32, 2, 1 << 16, 3);

    @Override
    public RegistrationResponseDto register(RegistrationRequestDto request) {
        // Check if the email exist
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }  

        // Save entity
        UserEntity saved = userRepository.save(
                UserEntity.builder()
                .username(request.getUsername())
                .passwordHash(encoder.encode(request.getPassword()))
                .build()
        );

        // Return dummy response
        return RegistrationResponseDto.builder()
                .userId(saved.getId())
                .username(saved.getUsername())
                .message("User registered successfully")
                .build();
    }
}
