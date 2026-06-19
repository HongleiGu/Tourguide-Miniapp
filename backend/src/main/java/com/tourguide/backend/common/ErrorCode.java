package com.tourguide.backend.common;

import lombok.Getter;

/**
 * Business / error codes carried in {@link ApiResponse#code()}.
 *
 * <p>Convention: {@code 0} = success; {@code 1xxxx} = client-side errors;
 * {@code 2xxxx} = server-side errors. Domain-specific codes are added per feature epic.
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),

    // ---- client errors (10xxx) ----
    BAD_REQUEST(10400, "请求参数错误"),
    UNAUTHORIZED(10401, "未认证"),
    FORBIDDEN(10403, "无权限"),
    NOT_FOUND(10404, "资源不存在"),
    CONFLICT(10409, "资源冲突"),

    // ---- server errors (20xxx) ----
    INTERNAL_ERROR(20500, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
