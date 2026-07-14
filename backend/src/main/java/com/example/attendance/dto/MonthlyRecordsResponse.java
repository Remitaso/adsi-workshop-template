package com.example.attendance.dto;

import java.util.List;

public record MonthlyRecordsResponse(
        String yearMonth,
        List<DailyRecord> records
) {
    public record DailyRecord(
            String workDate,
            String status,
            int totalWorkMinutes,
            int overtimeMinutes,
            List<TodayAttendanceResponse.EntryDto> entries
    ) {}
}
