package com.example.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public record TodayAttendanceResponse(
        LocalDate workDate,
        String status,
        List<EntryDto> entries,
        int totalWorkMinutes
) {
    public record EntryDto(
            Long id,
            String clockIn,
            String clockOut
    ) {}
}
