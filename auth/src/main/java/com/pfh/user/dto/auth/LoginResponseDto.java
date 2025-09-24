package com.pfh.user.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class LoginResponseDto {
    private String token;
    private String message;
    private Map<String, Object> claims; // userId, email, roles, exp
}
