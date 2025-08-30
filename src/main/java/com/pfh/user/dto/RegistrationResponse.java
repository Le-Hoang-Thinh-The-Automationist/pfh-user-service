package com.pfh.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationResponse {
    private Long userId;          
    private String registrationId; 
    private String username;
    private String message;
}
