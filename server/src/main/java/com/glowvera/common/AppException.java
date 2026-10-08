package com.glowvera.common;


public class AppException extends RuntimeException {

    private final ErrorCode code;
    private final transient Object details;

    public AppException(ErrorCode code, String message, Object details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public AppException(ErrorCode code, String message) {
        this(code, message, null);
    }

    public ErrorCode getCode() {
        return code;
    }

    public Object getDetails() {
        return details;
    }

    public static AppException badRequest(String message) {
        return new AppException(ErrorCode.BAD_REQUEST, message);
    }

    public static AppException badRequest(String message, Object details) {
        return new AppException(ErrorCode.BAD_REQUEST, message, details);
    }

    public static AppException unauthorized() {
        return new AppException(ErrorCode.UNAUTHORIZED, "Authentication required");
    }

    public static AppException unauthorized(String message) {
        return new AppException(ErrorCode.UNAUTHORIZED, message);
    }

    public static AppException forbidden() {
        return new AppException(ErrorCode.FORBIDDEN, "You do not have permission to do this");
    }

    public static AppException forbidden(String message) {
        return new AppException(ErrorCode.FORBIDDEN, message);
    }

    public static AppException notFound(String message) {
        return new AppException(ErrorCode.NOT_FOUND, message);
    }

    public static AppException conflict(String message) {
        return new AppException(ErrorCode.CONFLICT, message);
    }

    public static AppException conflict(String message, Object details) {
        return new AppException(ErrorCode.CONFLICT, message, details);
    }
}
