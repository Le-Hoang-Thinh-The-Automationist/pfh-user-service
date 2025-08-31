package com.pfh.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationResponseDto {
    private Long userId;          
    private String registrationId; 
    private String email;
    private String message;
}
