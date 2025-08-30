package com.pfh.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationRequestDto {
    
    @NotBlank
    private String username;
 
    @NotBlank
    @Email(message = "Invalid email format")    
    private String email;

    @NotBlank
    @Size(min = 12, message = "Password must be at least 12 characters long")
    private String password;
    
    @NotBlank
    private String confirmPassword;
}
