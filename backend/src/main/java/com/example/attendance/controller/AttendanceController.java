package com.example.attendance.controller;

import com.example.attendance.dto.ModifyEntryRequest;
import com.example.attendance.dto.MonthlyRecordsResponse;
import com.example.attendance.dto.TimeEntryResponse;
import com.example.attendance.dto.TodayAttendanceResponse;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.EmployeeIdResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeIdResolver employeeIdResolver;

    public AttendanceController(AttendanceService attendanceService,
                                EmployeeIdResolver employeeIdResolver) {
        this.attendanceService = attendanceService;
        this.employeeIdResolver = employeeIdResolver;
    }

    @PostMapping("/clock-in")
    public ResponseEntity<TimeEntryResponse> clockIn(@AuthenticationPrincipal UserDetails user) {
        Long employeeId = employeeIdResolver.resolve(user);
        var response = attendanceService.clockIn(employeeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/clock-out")
    public ResponseEntity<TimeEntryResponse> clockOut(@AuthenticationPrincipal UserDetails user) {
        Long employeeId = employeeIdResolver.resolve(user);
        var response = attendanceService.clockOut(employeeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today")
    public ResponseEntity<TodayAttendanceResponse> getToday(@AuthenticationPrincipal UserDetails user) {
        Long employeeId = employeeIdResolver.resolve(user);
        var response = attendanceService.getToday(employeeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/records")
    public ResponseEntity<MonthlyRecordsResponse> getMonthlyRecords(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam String yearMonth) {
        Long employeeId = employeeIdResolver.resolve(user);
        var response = attendanceService.getMonthlyRecords(employeeId, YearMonth.parse(yearMonth));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/entries/{entryId}")
    public ResponseEntity<TimeEntryResponse> modifyEntry(
            @PathVariable Long entryId,
            @Valid @RequestBody ModifyEntryRequest request) {
        var response = attendanceService.modifyEntry(entryId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/records/{recordId}/submit")
    public ResponseEntity<Void> submitForApproval(@PathVariable Long recordId) {
        attendanceService.submitForApproval(recordId);
        return ResponseEntity.ok().build();
    }
}
