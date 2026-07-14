package com.example.attendance.leave.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectLeaveRequest(
        @NotBlank String reason
) {
}
