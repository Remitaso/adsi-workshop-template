package com.example.attendance.controller;

import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.EmployeeIdResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approval")
public class ApprovalController {

    private final AttendanceService attendanceService;
    private final EmployeeIdResolver employeeIdResolver;

    public ApprovalController(AttendanceService attendanceService,
                              EmployeeIdResolver employeeIdResolver) {
        this.attendanceService = attendanceService;
        this.employeeIdResolver = employeeIdResolver;
    }

    @PostMapping("/time-records/{recordId}/approve")
    public ResponseEntity<Void> approveTimeRecord(
            @PathVariable Long recordId,
            @AuthenticationPrincipal UserDetails user) {
        Long approverId = employeeIdResolver.resolve(user);
        attendanceService.approve(recordId, approverId);
        return ResponseEntity.ok().build();
    }
}
