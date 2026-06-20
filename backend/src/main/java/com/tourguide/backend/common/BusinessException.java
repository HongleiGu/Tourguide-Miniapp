package com.tourguide.backend.common;

import lombok.Getter;

/**
 * Thrown for expected business-rule failures (e.g. group-buy full, slot unavailable).
 * Mapped to an {@link ApiResponse} by {@link GlobalExceptionHandler}.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
