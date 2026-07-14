package com.example.attendance.controller;

import com.example.attendance.config.SecurityConfig;
import com.example.attendance.exception.GlobalExceptionHandler;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.EmployeeIdResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApprovalController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    @MockitoBean
    private EmployeeIdResolver employeeIdResolver;

    @Test
    @DisplayName("POST /approval/time-records/{id}/approve: MANAGERで200")
    @WithMockUser(username = "suzuki@example.com", authorities = {"MANAGER"})
    void approve_asManager_returns200() throws Exception {
        when(employeeIdResolver.resolve(any())).thenReturn(2L);

        mockMvc.perform(post("/api/v1/approval/time-records/10/approve"))
                .andExpect(status().isOk());

        verify(attendanceService).approve(10L, 2L);
    }

    @Test
    @DisplayName("POST /approval/time-records/{id}/approve: HRで200")
    @WithMockUser(username = "sato@example.com", authorities = {"HR"})
    void approve_asHr_returns200() throws Exception {
        when(employeeIdResolver.resolve(any())).thenReturn(3L);

        mockMvc.perform(post("/api/v1/approval/time-records/10/approve"))
                .andExpect(status().isOk());

        verify(attendanceService).approve(10L, 3L);
    }

    @Test
    @DisplayName("POST /approval/time-records/{id}/approve: EMPLOYEEで403")
    @WithMockUser(username = "tanaka@example.com", authorities = {"EMPLOYEE"})
    void approve_asEmployee_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/approval/time-records/10/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /approval/time-records/{id}/approve: 未認証で401")
    void approve_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/approval/time-records/10/approve"))
                .andExpect(status().isUnauthorized());
    }
}
