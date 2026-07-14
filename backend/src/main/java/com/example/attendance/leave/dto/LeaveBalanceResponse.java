package com.example.attendance.leave.dto;

import java.math.BigDecimal;
import java.util.List;

public record LeaveBalanceResponse(
        BigDecimal totalRemaining,
        List<LeaveBalanceDetailResponse> details
) {
}
