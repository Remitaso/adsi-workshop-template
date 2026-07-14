package com.example.attendance.controller;

import com.example.attendance.config.SecurityConfig;
import com.example.attendance.config.CustomUserDetailsService;
import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.repository.EmployeeRepository;
import com.example.attendance.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@Import({SecurityConfig.class, CustomUserDetailsService.class})
@ActiveProfiles("test")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("HR権限で社員一覧が取得できる")
    @WithMockUser(authorities = "HR")
    void findAll_withHrAuthority_returnsOk() throws Exception {
        when(employeeService.findAll(null, null)).thenReturn(List.of(
                new EmployeeResponse(1L, "EMP001", "田中太郎", "tanaka@example.com", "EMPLOYEE", "開発1チーム")
        ));

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("田中太郎"));
    }

    @Test
    @DisplayName("EMPLOYEE権限では社員一覧にアクセスできない(403)")
    @WithMockUser(authorities = "EMPLOYEE")
    void findAll_withEmployeeAuthority_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未認証では社員一覧にアクセスできない(401)")
    void findAll_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized());
    }
}
