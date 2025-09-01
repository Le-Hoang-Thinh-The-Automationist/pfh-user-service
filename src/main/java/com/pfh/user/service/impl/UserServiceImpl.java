package com.pfh.user.service.impl;

import com.pfh.user.dto.RegistrationRequestDto;
import com.pfh.user.dto.RegistrationResponseDto;
import com.pfh.user.entity.UserEntity;
import com.pfh.user.repository.UserRepository;
import com.pfh.user.service.UserService;

import lombok.RequiredArgsConstructor;
import com.pfh.user.exception.DuplicateEmailException;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public RegistrationResponseDto createUser(RegistrationRequestDto request) {
        // Check if the email exist
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        // Save entity
        UserEntity saved = userRepository.save(
                UserEntity.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(request.getPassword())
                .build()
        );

        // Return dummy response
        return RegistrationResponseDto.builder()
                .userId(saved.getId())
                .email(saved.getEmail())
                .message("User registered successfully")
                .build();
    }
}
