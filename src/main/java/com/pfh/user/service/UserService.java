package com.pfh.user.service;

import com.pfh.user.dto.RegistrationRequestDto;
import com.pfh.user.dto.RegistrationResponseDto;

public interface UserService {
    RegistrationResponseDto registerUser(RegistrationRequestDto request);
}
