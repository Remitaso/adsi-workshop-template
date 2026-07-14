package com.example.attendance.leave.controller;

import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.leave.dto.LeaveBalanceDetailResponse;
import com.example.attendance.leave.dto.LeaveBalanceResponse;
import com.example.attendance.leave.entity.ApprovalStatus;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveType;
import com.example.attendance.leave.exception.InsufficientLeaveBalanceException;
import com.example.attendance.leave.service.LeaveService;
import com.example.attendance.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveService leaveService;

    @MockitoBean
    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(authService.getEmployeeByEmail("tanaka@example.com"))
                .thenReturn(new EmployeeResponse(1L, "EMP001", "田中太郎", "tanaka@example.com", "EMPLOYEE", "開発1チーム"));
        when(authService.getEmployeeByEmail("suzuki@example.com"))
                .thenReturn(new EmployeeResponse(2L, "MGR001", "鈴木一郎", "suzuki@example.com", "MANAGER", "開発1チーム"));
    }

    @Test
    @DisplayName("POST /api/v1/leaves: 正常申請 → 201")
    @WithMockUser(username = "tanaka@example.com")
    void postLeave_validRequest_returns201() throws Exception {
        LeaveRequest request = new LeaveRequest(1L, LeaveType.PAID,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20), "私用");

        when(leaveService.apply(anyLong(), eq(LeaveType.PAID), any(), any(), anyString()))
                .thenReturn(request);

        mockMvc.perform(post("/api/v1/leaves")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "leaveType": "PAID",
                                  "startDate": "2026-07-20",
                                  "endDate": "2026-07-20",
                                  "reason": "私用"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leaveType").value("PAID"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/leaves: 残高不足 → 409")
    @WithMockUser(username = "tanaka@example.com")
    void postLeave_insufficientBalance_returns409() throws Exception {
        when(leaveService.apply(anyLong(), eq(LeaveType.PAID), any(), any(), anyString()))
                .thenThrow(new InsufficientLeaveBalanceException("残高不足"));

        mockMvc.perform(post("/api/v1/leaves")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "leaveType": "PAID",
                                  "startDate": "2026-07-20",
                                  "endDate": "2026-07-25",
                                  "reason": "旅行"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/leaves: 自分の申請一覧")
    @WithMockUser(username = "tanaka@example.com")
    void getLeaves_returnsOwnRequests() throws Exception {
        LeaveRequest req = new LeaveRequest(1L, LeaveType.PAID,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20), "私用");

        when(leaveService.findByEmployee(anyLong(), any())).thenReturn(List.of(req));

        mockMvc.perform(get("/api/v1/leaves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].leaveType").value("PAID"));
    }

    @Test
    @DisplayName("GET /api/v1/leaves/balance: 残高照会")
    @WithMockUser(username = "tanaka@example.com")
    void getBalance_returnsBalanceSummary() throws Exception {
        LeaveBalanceResponse response = new LeaveBalanceResponse(
                new BigDecimal("12.0"),
                List.of(new LeaveBalanceDetailResponse(
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2028, 4, 1),
                        new BigDecimal("15.0"),
                        new BigDecimal("3.0"),
                        new BigDecimal("12.0")
                ))
        );
        when(leaveService.getBalance(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/v1/leaves/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRemaining").value(12.0))
                .andExpect(jsonPath("$.details[0].granted").value(15.0));
    }

    @Test
    @DisplayName("POST /api/v1/approval/leave-requests/{id}/approve: 承認 → 200")
    @WithMockUser(username = "suzuki@example.com", authorities = "MANAGER")
    void approveLeave_asManager_returns200() throws Exception {
        LeaveRequest request = new LeaveRequest(1L, LeaveType.PAID,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20), "私用");
        request.approve(2L);

        when(leaveService.approve(eq(1L), anyLong())).thenReturn(request);

        mockMvc.perform(post("/api/v1/approval/leave-requests/1/approve")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /api/v1/approval/leave-requests/{id}/reject: 却下 → 200")
    @WithMockUser(username = "suzuki@example.com", authorities = "MANAGER")
    void rejectLeave_asManager_returns200() throws Exception {
        LeaveRequest request = new LeaveRequest(1L, LeaveType.PAID,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20), "私用");
        request.reject(2L, "業務都合");

        when(leaveService.reject(eq(1L), anyLong(), eq("業務都合"))).thenReturn(request);

        mockMvc.perform(post("/api/v1/approval/leave-requests/1/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "業務都合"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/leaves/{id}: PENDING取消 → 204")
    @WithMockUser(username = "tanaka@example.com")
    void deleteLeave_pending_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/leaves/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
