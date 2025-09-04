package com.pfh.user.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationRequestDto {
 
    @Pattern(
        /* 
            Email format accepts:
                - eaxmple_user@example.com
            Email NOT accept:
                - invalid-email 
                - missing@domain 
                - @domain.com 
                - user@ 
                - user.domain.com 
        */ 
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "Invalid email format"
    ) 
    private String email;

    // All of password security shall be implemented in AuthService
    private String password;
    
    @NotBlank
    private String confirmPassword;
}
