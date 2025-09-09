package com.pfh.user.exception;

import org.springframework.http.HttpStatus;

import com.pfh.user.enums.UserStatus;

public class UserStatusException extends RuntimeException {
    private UserStatus status;

    public UserStatusException(UserStatus status) {
        super(errorMessage(status));
        this.status = status;
    }

    private static String errorMessage(UserStatus status) {
        return switch (status) {
            case UserStatus.LOCKED -> "Your account is locked. Please contact support.";
            case UserStatus.DISABLED -> "Your account is disabled. Please contact support.";
            case UserStatus.SUSPENDED -> "Your account is suspended. Please contact support.";
            case UserStatus.EXPIRED -> "Your account has expired. Please renew.";
            default -> "Unknown user status.";
        };
    }

    public HttpStatus getHttpStatus() {
        return switch (status) {
            case UserStatus.LOCKED -> HttpStatus.LOCKED; // Locked
            case UserStatus.DISABLED -> HttpStatus.FORBIDDEN; // Forbidden
            case UserStatus.SUSPENDED -> HttpStatus.FORBIDDEN; // Forbidden
            case UserStatus.EXPIRED -> HttpStatus.UNAUTHORIZED; // Unauthorized
            default -> HttpStatus.BAD_REQUEST; // Bad Request
        };
    }
}
