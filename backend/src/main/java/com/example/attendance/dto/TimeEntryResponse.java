package com.example.attendance.dto;

import com.example.attendance.entity.TimeEntry;

import java.time.LocalDateTime;

public record TimeEntryResponse(
        Long id,
        Long timeRecordId,
        LocalDateTime clockIn,
        LocalDateTime clockOut
) {
    public static TimeEntryResponse from(TimeEntry entity) {
        return new TimeEntryResponse(
                entity.getId(),
                entity.getTimeRecordId(),
                entity.getClockIn(),
                entity.getClockOut()
        );
    }
}
