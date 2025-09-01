package com.pfh.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
public class ErrorResponseDto {
    private int status;
    private String message;
    private Instant timestamp;
    private List<FieldErrorDto> errors;
}