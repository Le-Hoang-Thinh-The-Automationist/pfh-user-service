package com.pfh.user.service;

import com.pfh.user.dto.auth.RegistrationRequestDto;
import com.pfh.user.dto.auth.RegistrationResponseDto;


public interface UserService {
    RegistrationResponseDto createUser(RegistrationRequestDto request);
}
