package com.pfh.user.exception;

public class PasswordIsWeakException extends RuntimeException {
    public PasswordIsWeakException(String errorMessage) {
        super(errorMessage);
    }    
}