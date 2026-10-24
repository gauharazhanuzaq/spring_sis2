package com.example.sis1.web.exception;

import com.example.sis1.domain.exception.ResourceConflictException;
import com.example.sis1.domain.exception.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Central place that turns every exception the API can throw into the same
 * ApiError JSON shape, with the HTTP status code that matches the failure.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ---- 404 : entity not found -------------------------------------------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ApiError error = buildBaseError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ---- 409 : business-rule conflict (e.g. duplicate title) --------------------
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ResourceConflictException ex, HttpServletRequest request) {
        ApiError error = buildBaseError(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ---- 409 : DB-level unique constraint violation (race-condition fallback) ---
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                   HttpServletRequest request) {
        ApiError error = buildBaseError(HttpStatus.CONFLICT,
                "The request conflicts with the current state of the resource", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ---- 400 : @RequestParam / @PathVariable constraint violations --------------
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                                HttpServletRequest request) {
        List<ApiError.FieldValidationError> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> ApiError.FieldValidationError.builder()
                        .field(v.getPropertyPath().toString())
                        .message(v.getMessage())
                        .build())
                .collect(Collectors.toList());

        ApiError error = buildBaseError(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI());
        error.setErrors(fieldErrors);
        return ResponseEntity.badRequest().body(error);
    }

    // ---- 400 : wrong path-variable type, e.g. GET /api/tasks/abc ----------------
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                         HttpServletRequest request) {
        String message = String.format("Parameter '%s' should be of type %s", ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        ApiError error = buildBaseError(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    // ---- 400 : generic illegal argument raised by service code -------------------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ApiError error = buildBaseError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    // ---- 500 : anything unmapped, last line of defence ---------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        ApiError error = buildBaseError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error occurred: " + ex.getMessage(), request.getRequestURI());
        return ResponseEntity.internalServerError().body(error);
    }

    // ---- overrides for framework-level exceptions, kept in the same ApiError shape ----

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                    HttpHeaders headers,
                                                                    HttpStatusCode status,
                                                                    WebRequest request) {
        List<ApiError.FieldValidationError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());

        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed for one or more fields")
                .path(extractPath(request))
                .errors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                    HttpHeaders headers,
                                                                    HttpStatusCode status,
                                                                    WebRequest request) {
        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Malformed JSON request body or invalid field value")
                .path(extractPath(request))
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    // ---- helpers ------------------------------------------------------------------

    private ApiError.FieldValidationError toFieldError(FieldError fe) {
        return ApiError.FieldValidationError.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .build();
    }

    private ApiError buildBaseError(HttpStatus status, String message, String path) {
        return ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
