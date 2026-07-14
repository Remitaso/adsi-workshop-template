package com.example.attendance.controller;

import com.example.attendance.config.SecurityConfig;
import com.example.attendance.config.CustomUserDetailsService;
import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.entity.Employee;
import com.example.attendance.entity.Role;
import com.example.attendance.repository.EmployeeRepository;
import com.example.attendance.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CustomUserDetailsService.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("ログイン: 正しい認証情報で200とユーザー情報が返される")
    void login_validCredentials_returnsOk() throws Exception {
        var employee = Employee.builder()
                .id(1L).employeeCode("EMP001").name("田中太郎")
                .email("tanaka@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.EMPLOYEE).teamId(1L).active(true)
                .build();
        when(employeeRepository.findByEmail("tanaka@example.com")).thenReturn(Optional.of(employee));
        when(authService.getEmployeeByEmail("tanaka@example.com")).thenReturn(
                new EmployeeResponse(1L, "EMP001", "田中太郎", "tanaka@example.com", "EMPLOYEE", "開発1チーム")
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"tanaka@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("田中太郎"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("ログイン: 間違ったパスワードで401が返される")
    void login_invalidPassword_returnsUnauthorized() throws Exception {
        var employee = Employee.builder()
                .id(1L).email("tanaka@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.EMPLOYEE).active(true)
                .build();
        when(employeeRepository.findByEmail("tanaka@example.com")).thenReturn(Optional.of(employee));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"tanaka@example.com","password":"wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("認証なしでAPI呼び出すと401が返される")
    void me_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
