package com.example.attendance.leave.controller;

import com.example.attendance.leave.dto.*;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.exception.InsufficientLeaveBalanceException;
import com.example.attendance.leave.service.LeaveService;
import com.example.attendance.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class LeaveController {

    private final LeaveService leaveService;
    private final AuthService authService;

    public LeaveController(LeaveService leaveService, AuthService authService) {
        this.leaveService = leaveService;
        this.authService = authService;
    }

    @PostMapping("/leaves")
    public ResponseEntity<LeaveResponse> apply(
            @Valid @RequestBody CreateLeaveRequest request,
            Authentication authentication) {
        Long employeeId = getEmployeeId(authentication);
        LeaveRequest result = leaveService.apply(
                employeeId, request.leaveType(),
                request.startDate(), request.endDate(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(LeaveResponse.from(result));
    }

    @GetMapping("/leaves")
    public ResponseEntity<Map<String, List<LeaveResponse>>> getLeaves(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        Long employeeId = getEmployeeId(authentication);
        List<LeaveRequest> requests = leaveService.findByEmployee(employeeId, status);
        List<LeaveResponse> items = requests.stream().map(LeaveResponse::from).toList();
        return ResponseEntity.ok(Map.of("items", items));
    }

    @DeleteMapping("/leaves/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id, Authentication authentication) {
        Long employeeId = getEmployeeId(authentication);
        leaveService.cancel(id, employeeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/leaves/balance")
    public ResponseEntity<LeaveBalanceResponse> getBalance(Authentication authentication) {
        Long employeeId = getEmployeeId(authentication);
        return ResponseEntity.ok(leaveService.getBalance(employeeId));
    }

    @GetMapping("/approval/leave-requests")
    public ResponseEntity<Map<String, List<LeaveResponse>>> getPendingLeaves(
            Authentication authentication) {
        Long managerId = getEmployeeId(authentication);
        List<LeaveRequest> requests = leaveService.findPendingByManager(managerId);
        List<LeaveResponse> items = requests.stream().map(LeaveResponse::from).toList();
        return ResponseEntity.ok(Map.of("items", items));
    }

    @PostMapping("/approval/leave-requests/{requestId}/approve")
    public ResponseEntity<LeaveResponse> approve(
            @PathVariable Long requestId,
            Authentication authentication) {
        Long managerId = getEmployeeId(authentication);
        LeaveRequest result = leaveService.approve(requestId, managerId);
        return ResponseEntity.ok(LeaveResponse.from(result));
    }

    @PostMapping("/approval/leave-requests/{requestId}/reject")
    public ResponseEntity<LeaveResponse> reject(
            @PathVariable Long requestId,
            @Valid @RequestBody RejectLeaveRequest request,
            Authentication authentication) {
        Long managerId = getEmployeeId(authentication);
        LeaveRequest result = leaveService.reject(requestId, managerId, request.reason());
        return ResponseEntity.ok(LeaveResponse.from(result));
    }

    @ExceptionHandler(InsufficientLeaveBalanceException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientBalance(
            InsufficientLeaveBalanceException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }

    private Long getEmployeeId(Authentication authentication) {
        return authService.getEmployeeByEmail(authentication.getName()).id();
    }
}
