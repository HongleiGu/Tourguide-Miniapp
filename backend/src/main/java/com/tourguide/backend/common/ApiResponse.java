package com.tourguide.backend.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API response envelope returned by every endpoint: {@code { code, message, data }}.
 *
 * <p>{@code code} is a business code (see {@link ErrorCode}); {@code 0} means success.
 * {@code data} is omitted from the JSON when null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> error(ErrorCode error) {
        return new ApiResponse<>(error.getCode(), error.getMessage(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode error, String message) {
        return new ApiResponse<>(error.getCode(), message, null);
    }
}
