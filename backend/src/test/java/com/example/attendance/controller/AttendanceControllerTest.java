package com.example.attendance.controller;

import com.example.attendance.config.SecurityConfig;
import com.example.attendance.dto.ModifyEntryRequest;
import com.example.attendance.dto.MonthlyRecordsResponse;
import com.example.attendance.dto.TimeEntryResponse;
import com.example.attendance.dto.TodayAttendanceResponse;
import com.example.attendance.exception.InvalidOperationException;
import com.example.attendance.exception.GlobalExceptionHandler;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.EmployeeIdResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    @MockitoBean
    private EmployeeIdResolver employeeIdResolver;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    @DisplayName("POST /clock-in: 201で打刻レスポンスを返す")
    @WithMockUser(username = "tanaka@example.com")
    void clockIn_returns201() throws Exception {
        when(employeeIdResolver.resolve(any())).thenReturn(1L);
        when(attendanceService.clockIn(1L)).thenReturn(
                new TimeEntryResponse(1L, 10L, LocalDateTime.of(2026, 7, 14, 9, 0), null));

        mockMvc.perform(post("/api/v1/attendance/clock-in"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.timeRecordId").value(10))
                .andExpect(jsonPath("$.clockOut").isEmpty());
    }

    @Test
    @DisplayName("POST /clock-in: 出勤中に再打刻で409")
    @WithMockUser(username = "tanaka@example.com")
    void clockIn_alreadyClockedIn_returns409() throws Exception {
        when(employeeIdResolver.resolve(any())).thenReturn(1L);
        when(attendanceService.clockIn(1L))
                .thenThrow(new InvalidOperationException("既に出勤中です。"));

        mockMvc.perform(post("/api/v1/attendance/clock-in"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_OPERATION"));
    }

    @Test
    @DisplayName("POST /clock-out: 200で退勤レスポンスを返す")
    @WithMockUser(username = "tanaka@example.com")
    void clockOut_returns200() throws Exception {
        when(employeeIdResolver.resolve(any())).thenReturn(1L);
        when(attendanceService.clockOut(1L)).thenReturn(
                new TimeEntryResponse(1L, 10L,
                        LocalDateTime.of(2026, 7, 14, 9, 0),
                        LocalDateTime.of(2026, 7, 14, 18, 0)));

        mockMvc.perform(post("/api/v1/attendance/clock-out"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clockOut").isNotEmpty());
    }

    @Test
    @DisplayName("GET /today: 200で本日の勤怠を返す")
    @WithMockUser(username = "tanaka@example.com")
    void getToday_returns200() throws Exception {
        when(employeeIdResolver.resolve(any())).thenReturn(1L);
        when(attendanceService.getToday(1L)).thenReturn(
                new TodayAttendanceResponse(
                        LocalDate.of(2026, 7, 14), "DRAFT",
                        List.of(new TodayAttendanceResponse.EntryDto(1L, "2026-07-14T09:00:00", null)),
                        0));

        mockMvc.perform(get("/api/v1/attendance/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workDate").value("2026-07-14"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.entries[0].id").value(1));
    }

    @Test
    @DisplayName("GET /records: 200で月次レスポンスを返す")
    @WithMockUser(username = "tanaka@example.com")
    void getMonthlyRecords_returns200() throws Exception {
        when(employeeIdResolver.resolve(any())).thenReturn(1L);
        when(attendanceService.getMonthlyRecords(eq(1L), eq(YearMonth.of(2026, 7))))
                .thenReturn(new MonthlyRecordsResponse("2026-07", List.of()));

        mockMvc.perform(get("/api/v1/attendance/records")
                        .param("yearMonth", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2026-07"));
    }

    @Test
    @DisplayName("PUT /entries/{id}: DRAFT状態で200")
    @WithMockUser(username = "tanaka@example.com")
    void modifyEntry_draft_returns200() throws Exception {
        var request = new ModifyEntryRequest(
                LocalDateTime.of(2026, 7, 14, 9, 15),
                LocalDateTime.of(2026, 7, 14, 18, 30));
        when(employeeIdResolver.resolve(any())).thenReturn(1L);
        when(attendanceService.modifyEntry(eq(1L), eq(1L), any())).thenReturn(
                new TimeEntryResponse(1L, 10L,
                        LocalDateTime.of(2026, 7, 14, 9, 15),
                        LocalDateTime.of(2026, 7, 14, 18, 30)));

        mockMvc.perform(put("/api/v1/attendance/entries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clockIn").value("2026-07-14T09:15:00"));
    }

    @Test
    @DisplayName("PUT /entries/{id}: APPROVED状態で409")
    @WithMockUser(username = "tanaka@example.com")
    void modifyEntry_approved_returns409() throws Exception {
        var request = new ModifyEntryRequest(
                LocalDateTime.of(2026, 7, 14, 9, 15), null);
        when(employeeIdResolver.resolve(any())).thenReturn(1L);
        when(attendanceService.modifyEntry(eq(1L), eq(1L), any()))
                .thenThrow(new InvalidOperationException("承認済みの記録は修正できません。"));

        mockMvc.perform(put("/api/v1/attendance/entries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /records/{id}/submit: 200")
    @WithMockUser(username = "tanaka@example.com")
    void submitForApproval_returns200() throws Exception {
        when(employeeIdResolver.resolve(any())).thenReturn(1L);
        mockMvc.perform(post("/api/v1/attendance/records/10/submit"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("未認証アクセス: 401")
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/clock-in"))
                .andExpect(status().isUnauthorized());
    }
}
