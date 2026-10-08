package com.glowvera.web;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.glowvera.common.AppException;
import com.glowvera.common.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> handleApp(AppException ex) {
        return respond(ex.getCode(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleInvalidBody(MethodArgumentNotValidException ex) {
        return validation(ex.getBindingResult());
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ApiResponse<Void>> handleBind(BindException ex) {
        return validation(ex.getBindingResult());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        List<Map<String, String>> details = ex.getConstraintViolations().stream()
                .map(v -> Map.of("field", v.getPropertyPath().toString(), "message", v.getMessage()))
                .toList();
        return respond(ErrorCode.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException ex) {
        List<Map<String, String>> details = ex.getAllErrors().stream()
                .map(e -> Map.of("field", "request", "message",
                        e.getDefaultMessage() == null ? "Invalid value" : e.getDefaultMessage()))
                .toList();
        return respond(ErrorCode.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException ife) {
            String field = ife.getPath().stream().map(r -> r.getFieldName()).filter(n -> n != null)
                    .reduce((a, b) -> a + "." + b).orElse("body");
            return respond(ErrorCode.BAD_REQUEST, "Validation failed",
                    List.of(Map.of("field", field, "message", "Invalid value")));
        }
        return respond(ErrorCode.INVALID_JSON, "Request body is not valid JSON", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(ErrorCode.BAD_REQUEST, "Validation failed",
                List.of(Map.of("field", ex.getName(), "message", "Invalid value")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return respond(ErrorCode.DUPLICATE, "A record with this value already exists", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex) {
        return respond(ErrorCode.UNAUTHORIZED, "Authentication required", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handleDenied(AccessDeniedException ex) {
        return respond(ErrorCode.FORBIDDEN, "You do not have permission to do this", null);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex) {
        return respond(ErrorCode.NOT_FOUND, "Route not found", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(ErrorCode.INTERNAL_ERROR, "Something went wrong. Please try again later.", null);
    }

    private ResponseEntity<ApiResponse<Void>> validation(BindingResult result) {
        List<Map<String, String>> details = result.getFieldErrors().stream()
                .map((FieldError e) -> Map.of(
                        "field", e.getField(),
                        "message", e.getDefaultMessage() == null ? "Invalid value" : e.getDefaultMessage()))
                .toList();
        return respond(ErrorCode.BAD_REQUEST, "Validation failed", details);
    }

    private static ResponseEntity<ApiResponse<Void>> respond(ErrorCode code, String message, Object details) {
        return ResponseEntity.status(HttpStatus.valueOf(code.httpStatus()))
                .body(ApiResponse.failure(code.name(), message, details));
    }
}
