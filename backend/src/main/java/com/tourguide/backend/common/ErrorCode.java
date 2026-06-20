package com.tourguide.backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Business / error codes carried in {@link ApiResponse#code()}, each mapped to the HTTP status
 * the response is sent with.
 *
 * <p>Convention: {@code 0} = success; {@code 1xxxx} = client-side errors;
 * {@code 2xxxx} = server-side errors (last three digits mirror the HTTP status).
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, HttpStatus.OK, "success"),

    // ---- client errors (10xxx) ----
    BAD_REQUEST(10400, HttpStatus.BAD_REQUEST, "请求参数错误"),
    UNAUTHORIZED(10401, HttpStatus.UNAUTHORIZED, "未认证"),
    FORBIDDEN(10403, HttpStatus.FORBIDDEN, "无权限"),
    NOT_FOUND(10404, HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(10409, HttpStatus.CONFLICT, "资源冲突"),

    // ---- server errors (20xxx) ----
    INTERNAL_ERROR(20500, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
