package com.example.attendance.domain;

import com.example.attendance.entity.TimeEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkDurationTest {

    @Test
    @DisplayName("8時間勤務: 全て所定内時間")
    void calculate_8hours_allRegular() {
        var entries = List.of(entry("2026-07-14T09:00", "2026-07-14T17:00"));

        var duration = WorkDuration.calculate(entries);

        assertThat(duration.totalMinutes()).isEqualTo(480);
        assertThat(duration.regularMinutes()).isEqualTo(480);
        assertThat(duration.overtimeMinutes()).isEqualTo(0);
        assertThat(duration.nightMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("9時間勤務: 8h所定内 + 1h残業")
    void calculate_9hours_1hourOvertime() {
        var entries = List.of(entry("2026-07-14T09:00", "2026-07-14T18:00"));

        var duration = WorkDuration.calculate(entries);

        assertThat(duration.totalMinutes()).isEqualTo(540);
        assertThat(duration.regularMinutes()).isEqualTo(480);
        assertThat(duration.overtimeMinutes()).isEqualTo(60);
        assertThat(duration.nightMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("深夜帯勤務(22:00-23:00): 深夜時間に加算")
    void calculate_nightWork_nightMinutes() {
        var entries = List.of(entry("2026-07-14T20:00", "2026-07-14T23:00"));

        var duration = WorkDuration.calculate(entries);

        assertThat(duration.totalMinutes()).isEqualTo(180);
        assertThat(duration.nightMinutes()).isEqualTo(60);
    }

    @Test
    @DisplayName("早朝深夜帯(3:00-6:00): 5:00まで深夜")
    void calculate_earlyMorning_nightUntil5() {
        var entries = List.of(entry("2026-07-14T03:00", "2026-07-14T06:00"));

        var duration = WorkDuration.calculate(entries);

        assertThat(duration.totalMinutes()).isEqualTo(180);
        assertThat(duration.nightMinutes()).isEqualTo(120);
    }

    @Test
    @DisplayName("複数エントリ合算: 中抜けあり")
    void calculate_multipleEntries_summed() {
        var entries = List.of(
                entry("2026-07-14T09:00", "2026-07-14T12:00"),
                entry("2026-07-14T13:00", "2026-07-14T18:00")
        );

        var duration = WorkDuration.calculate(entries);

        assertThat(duration.totalMinutes()).isEqualTo(480);
        assertThat(duration.regularMinutes()).isEqualTo(480);
        assertThat(duration.overtimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("複数エントリで残業発生")
    void calculate_multipleEntries_withOvertime() {
        var entries = List.of(
                entry("2026-07-14T09:00", "2026-07-14T12:00"),
                entry("2026-07-14T13:00", "2026-07-14T19:00")
        );

        var duration = WorkDuration.calculate(entries);

        assertThat(duration.totalMinutes()).isEqualTo(540);
        assertThat(duration.regularMinutes()).isEqualTo(480);
        assertThat(duration.overtimeMinutes()).isEqualTo(60);
    }

    @Test
    @DisplayName("退勤未打刻のエントリは計算から除外")
    void calculate_nullClockOut_excluded() {
        var entries = List.of(
                entry("2026-07-14T09:00", "2026-07-14T12:00"),
                entryNoClockOut("2026-07-14T13:00")
        );

        var duration = WorkDuration.calculate(entries);

        assertThat(duration.totalMinutes()).isEqualTo(180);
    }

    @Test
    @DisplayName("空リスト: 全て0")
    void calculate_emptyList_allZero() {
        var duration = WorkDuration.calculate(List.of());

        assertThat(duration.totalMinutes()).isEqualTo(0);
        assertThat(duration.regularMinutes()).isEqualTo(0);
        assertThat(duration.overtimeMinutes()).isEqualTo(0);
        assertThat(duration.nightMinutes()).isEqualTo(0);
    }

    private TimeEntry entry(String clockIn, String clockOut) {
        return TimeEntry.builder()
                .clockIn(LocalDateTime.parse(clockIn))
                .clockOut(LocalDateTime.parse(clockOut))
                .build();
    }

    private TimeEntry entryNoClockOut(String clockIn) {
        return TimeEntry.builder()
                .clockIn(LocalDateTime.parse(clockIn))
                .build();
    }
}
