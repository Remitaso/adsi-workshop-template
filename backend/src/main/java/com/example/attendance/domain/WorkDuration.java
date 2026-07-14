package com.example.attendance.domain;

import com.example.attendance.entity.TimeEntry;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record WorkDuration(
        int totalMinutes,
        int regularMinutes,
        int overtimeMinutes,
        int nightMinutes
) {

    private static final int REGULAR_MINUTES_PER_DAY = 480;
    private static final LocalTime NIGHT_START = LocalTime.of(22, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(5, 0);

    public static WorkDuration calculate(List<TimeEntry> entries) {
        int totalMinutes = 0;
        int nightMinutes = 0;

        for (TimeEntry entry : entries) {
            if (entry.getClockOut() == null) {
                continue;
            }
            long minutes = ChronoUnit.MINUTES.between(entry.getClockIn(), entry.getClockOut());
            totalMinutes += (int) minutes;
            nightMinutes += calculateNightMinutes(entry.getClockIn(), entry.getClockOut());
        }

        int regularMinutes = Math.min(totalMinutes, REGULAR_MINUTES_PER_DAY);
        int overtimeMinutes = Math.max(0, totalMinutes - REGULAR_MINUTES_PER_DAY);

        return new WorkDuration(totalMinutes, regularMinutes, overtimeMinutes, nightMinutes);
    }

    private static int calculateNightMinutes(LocalDateTime start, LocalDateTime end) {
        int nightMinutes = 0;
        LocalDateTime current = start;

        while (current.isBefore(end)) {
            LocalTime time = current.toLocalTime();
            if (isNightTime(time)) {
                nightMinutes++;
            }
            current = current.plusMinutes(1);
        }

        return nightMinutes;
    }

    private static boolean isNightTime(LocalTime time) {
        return !time.isBefore(NIGHT_START) || time.isBefore(NIGHT_END);
    }
}
