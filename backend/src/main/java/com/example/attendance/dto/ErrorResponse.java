package com.example.attendance.dto;

import java.util.List;

public record ErrorResponse(
        ErrorBody error
) {
    public record ErrorBody(
            String code,
            String message,
            List<FieldError> details
    ) {}

    public record FieldError(
            String field,
            String message
    ) {}

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message, List.of()));
    }

    public static ErrorResponse withDetails(String code, String message, List<FieldError> details) {
        return new ErrorResponse(new ErrorBody(code, message, details));
    }
}
