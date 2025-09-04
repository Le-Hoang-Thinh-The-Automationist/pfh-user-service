package com.pfh.user.service;

import com.pfh.user.dto.auth.RegistrationRequestDto;
import com.pfh.user.dto.auth.RegistrationResponseDto;
import com.pfh.user.entity.UserEntity;

public interface UserService {
    RegistrationResponseDto createUser(RegistrationRequestDto request);
    UserEntity getUserById(long id);
    UserEntity getUserByEmail(String email);
}
