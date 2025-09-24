package com.pfh.user.service;

import com.pfh.user.dto.auth.LoginRequestDto;
import com.pfh.user.dto.auth.LoginResponseDto;
import com.pfh.user.dto.auth.RegistrationRequestDto;
import com.pfh.user.dto.auth.RegistrationResponseDto;

public interface AuthService {
    RegistrationResponseDto register(RegistrationRequestDto request);
    LoginResponseDto login(LoginRequestDto request, String ip, String userAgent);
}
