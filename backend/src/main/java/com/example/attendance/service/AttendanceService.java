package com.example.attendance.service;

import com.example.attendance.dto.ModifyEntryRequest;
import com.example.attendance.dto.MonthlyRecordsResponse;
import com.example.attendance.dto.TimeEntryResponse;
import com.example.attendance.dto.TodayAttendanceResponse;

import java.time.YearMonth;

public interface AttendanceService {

    TimeEntryResponse clockIn(Long employeeId);

    TimeEntryResponse clockOut(Long employeeId);

    TodayAttendanceResponse getToday(Long employeeId);

    MonthlyRecordsResponse getMonthlyRecords(Long employeeId, YearMonth yearMonth);

    TimeEntryResponse modifyEntry(Long entryId, ModifyEntryRequest request);

    void submitForApproval(Long timeRecordId);

    void approve(Long timeRecordId, Long approverId);
}
