package com.pfh.user.exception;

import com.pfh.user.dto.ErrorResponseDto;
import com.pfh.user.dto.FieldErrorDto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /******************************* INPUT FORMAT HANDLING   *******************************/
    // HTTP message not readable
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(HttpMessageNotReadableException ex) {
        FieldErrorDto fieldError = new FieldErrorDto("syntax", "Malformed JSON");

        ErrorResponseDto errorResponse = new ErrorResponseDto(
            HttpStatus.BAD_REQUEST.value(),
            "Malformed JSON",
            Instant.now(),
            Collections.singletonList(fieldError)
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // Request input are invalid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<FieldErrorDto> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldErrorDto(error.getField(), error.getDefaultMessage()))
            .collect(Collectors.toList());

        ErrorResponseDto errorResponse = new ErrorResponseDto(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            Instant.now(),
            fieldErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    /******************************* EMAIL HANDLING   *******************************/
    // Handle duplicate email
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateEmail(DuplicateEmailException ex) {
        FieldErrorDto fieldError = new FieldErrorDto("email", "Email already registered");

        ErrorResponseDto errorResponse = new ErrorResponseDto(
            HttpStatus.CONFLICT.value(),          
            ex.getMessage(),                      
            Instant.now(),                        
            Collections.singletonList(fieldError) 
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }



    /******************************* PASSWORD HANDLING   *******************************/
    // Password Mismatch
    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handlePasswordMismatch(PasswordMismatchException ex) {
        FieldErrorDto fieldError = new FieldErrorDto("confirmPassword", ex.getMessage());

        ErrorResponseDto errorResponse = new ErrorResponseDto(
            HttpStatus.BAD_REQUEST.value(),          
            ex.getMessage(),                      
            Instant.now(),                        
            Collections.singletonList(fieldError) 
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // Password is too weak
    @ExceptionHandler(PasswordIsWeakException.class)
    public ResponseEntity<ErrorResponseDto> handlePasswordMismatch(PasswordIsWeakException ex) {
        FieldErrorDto fieldError = new FieldErrorDto("password", "Password is too weak");

        ErrorResponseDto errorResponse = new ErrorResponseDto(
            HttpStatus.BAD_REQUEST.value(),          
            ex.getMessage(),                      
            Instant.now(),                        
            Collections.singletonList(fieldError) 
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

}
