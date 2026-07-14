package com.example.attendance.leave.service;

import com.example.attendance.leave.dto.LeaveBalanceResponse;
import com.example.attendance.leave.entity.LeaveBalance;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LeaveService {

    LeaveRequest apply(Long employeeId, LeaveType leaveType,
                       LocalDate startDate, LocalDate endDate, String reason);

    void cancel(Long requestId, Long employeeId);

    List<LeaveRequest> findByEmployee(Long employeeId, String status);

    LeaveRequest approve(Long requestId, Long approverId);

    LeaveRequest reject(Long requestId, Long approverId, String reason);

    LeaveBalanceResponse getBalance(Long employeeId);

    LeaveBalance grantAnnualLeave(Long employeeId, LocalDate grantDate);

    BigDecimal calculateLeaveDays(LeaveType leaveType, LocalDate startDate, LocalDate endDate);

    List<LeaveRequest> findPendingByManager(Long managerId);
}
