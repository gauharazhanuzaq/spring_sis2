package com.example.sis1.web.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Single, unified shape for every error response the API returns.
 *
 * Example (404):
 * {
 *   "timestamp": "2026-09-27T10:15:30",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Task with id 42 not found",
 *   "path": "/api/tasks/42"
 * }
 *
 * Example (400, validation failure):
 * {
 *   "timestamp": "2026-09-27T10:16:02",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed for one or more fields",
 *   "path": "/api/tasks",
 *   "errors": [
 *     { "field": "title", "message": "Title length must be between 3 and 100 characters" }
 *   ]
 * }
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private int status;
    private String error;
    private String message;
    private String path;

    /** Populated only for Bean Validation failures; omitted from the JSON otherwise. */
    private List<FieldValidationError> errors;

    @Data
    @Builder
    public static class FieldValidationError {
        private String field;
        private String message;
    }
}
