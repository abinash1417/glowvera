package com.glowvera.common;

public enum ErrorCode {
    BAD_REQUEST(400),
    INVALID_JSON(400),
    UNAUTHORIZED(401),
    INVALID_CREDENTIALS(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    CONFLICT(409),
    DUPLICATE(409),
    RATE_LIMITED(429),
    INTERNAL_ERROR(500),
    PAYMENT_UNAVAILABLE(503),
    WHATSAPP_UNAVAILABLE(503);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
