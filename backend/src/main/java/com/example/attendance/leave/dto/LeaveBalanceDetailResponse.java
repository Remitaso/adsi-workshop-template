package com.example.attendance.leave.dto;

import com.example.attendance.leave.entity.LeaveBalance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveBalanceDetailResponse(
        LocalDate grantDate,
        LocalDate expiryDate,
        BigDecimal granted,
        BigDecimal used,
        BigDecimal remaining
) {
    public static LeaveBalanceDetailResponse from(LeaveBalance entity) {
        return new LeaveBalanceDetailResponse(
                entity.getGrantDate(),
                entity.getExpiryDate(),
                entity.getGrantedDays(),
                entity.getUsedDays(),
                entity.getRemaining()
        );
    }
}
