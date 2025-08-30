package com.pfh.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationRequestDto {
    private String username;
    private String password;
    private String email;
    private String confirmPassword;
}
