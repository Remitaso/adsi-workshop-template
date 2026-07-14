package com.example.attendance.leave.dto;

import com.example.attendance.leave.entity.ApprovalStatus;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveResponse(
        Long id,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        ApprovalStatus status,
        String rejectReason,
        LocalDateTime createdAt
) {
    public static LeaveResponse from(LeaveRequest entity) {
        return new LeaveResponse(
                entity.getId(),
                entity.getLeaveType(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getReason(),
                entity.getStatus(),
                entity.getRejectReason(),
                entity.getCreatedAt()
        );
    }
}
