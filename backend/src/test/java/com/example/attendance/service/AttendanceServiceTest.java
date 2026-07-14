package com.example.attendance.service;

import com.example.attendance.domain.WorkDuration;
import com.example.attendance.dto.ModifyEntryRequest;
import com.example.attendance.entity.RecordStatus;
import com.example.attendance.entity.TimeEntry;
import com.example.attendance.entity.TimeRecord;
import com.example.attendance.exception.InvalidOperationException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.TimeEntryRepository;
import com.example.attendance.repository.TimeRecordRepository;
import com.example.attendance.service.impl.AttendanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private TimeRecordRepository timeRecordRepository;

    @Mock
    private TimeEntryRepository timeEntryRepository;

    private AttendanceService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceServiceImpl(timeRecordRepository, timeEntryRepository);
    }

    @Nested
    @DisplayName("clockIn")
    class ClockIn {

        @Test
        @DisplayName("当日未打刻: TimeRecord作成 + TimeEntry作成")
        void clockIn_noRecordToday_createsRecordAndEntry() {
            Long employeeId = 1L;
            when(timeRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
                    .thenReturn(Optional.empty());
            when(timeRecordRepository.save(any(TimeRecord.class)))
                    .thenAnswer(inv -> {
                        TimeRecord r = inv.getArgument(0);
                        r.setId(10L);
                        return r;
                    });
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(inv -> {
                        TimeEntry e = inv.getArgument(0);
                        e.setId(1L);
                        return e;
                    });

            var result = service.clockIn(employeeId);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.timeRecordId()).isEqualTo(10L);
            assertThat(result.clockIn()).isNotNull();
            assertThat(result.clockOut()).isNull();
        }

        @Test
        @DisplayName("既存レコードあり&退勤済み: 再出勤(中抜け後)としてEntryのみ作成")
        void clockIn_existingRecordAllClockedOut_createsNewEntry() {
            Long employeeId = 1L;
            var record = TimeRecord.builder().id(10L).employeeId(employeeId)
                    .workDate(LocalDate.now()).status(RecordStatus.DRAFT).build();
            var existingEntry = TimeEntry.builder().id(1L).timeRecordId(10L)
                    .clockIn(LocalDateTime.now().minusHours(3))
                    .clockOut(LocalDateTime.now().minusHours(1))
                    .build();

            when(timeRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
                    .thenReturn(Optional.of(record));
            when(timeEntryRepository.findByTimeRecordIdOrderByClockIn(10L))
                    .thenReturn(List.of(existingEntry));
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(inv -> {
                        TimeEntry e = inv.getArgument(0);
                        e.setId(2L);
                        return e;
                    });

            var result = service.clockIn(employeeId);

            assertThat(result.id()).isEqualTo(2L);
            assertThat(result.clockOut()).isNull();
        }

        @Test
        @DisplayName("出勤中(clockOutがnull)のEntryがある: エラー")
        void clockIn_alreadyClockedIn_throwsException() {
            Long employeeId = 1L;
            var record = TimeRecord.builder().id(10L).employeeId(employeeId)
                    .workDate(LocalDate.now()).status(RecordStatus.DRAFT).build();
            var openEntry = TimeEntry.builder().id(1L).timeRecordId(10L)
                    .clockIn(LocalDateTime.now().minusHours(1))
                    .clockOut(null)
                    .build();

            when(timeRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
                    .thenReturn(Optional.of(record));
            when(timeEntryRepository.findByTimeRecordIdOrderByClockIn(10L))
                    .thenReturn(List.of(openEntry));

            assertThatThrownBy(() -> service.clockIn(employeeId))
                    .isInstanceOf(InvalidOperationException.class);
        }
    }

    @Nested
    @DisplayName("clockOut")
    class ClockOut {

        @Test
        @DisplayName("出勤中のEntryがある: clockOutが記録される")
        void clockOut_openEntry_recordsClockOut() {
            Long employeeId = 1L;
            var record = TimeRecord.builder().id(10L).employeeId(employeeId)
                    .workDate(LocalDate.now()).status(RecordStatus.DRAFT).build();
            var openEntry = TimeEntry.builder().id(1L).timeRecordId(10L)
                    .clockIn(LocalDateTime.now().minusHours(1))
                    .clockOut(null)
                    .build();

            when(timeRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
                    .thenReturn(Optional.of(record));
            when(timeEntryRepository.findByTimeRecordIdOrderByClockIn(10L))
                    .thenReturn(List.of(openEntry));
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            var result = service.clockOut(employeeId);

            assertThat(result.clockOut()).isNotNull();
        }

        @Test
        @DisplayName("出勤中のEntryがない: エラー")
        void clockOut_noOpenEntry_throwsException() {
            Long employeeId = 1L;
            var record = TimeRecord.builder().id(10L).employeeId(employeeId)
                    .workDate(LocalDate.now()).status(RecordStatus.DRAFT).build();
            var closedEntry = TimeEntry.builder().id(1L).timeRecordId(10L)
                    .clockIn(LocalDateTime.now().minusHours(2))
                    .clockOut(LocalDateTime.now().minusHours(1))
                    .build();

            when(timeRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
                    .thenReturn(Optional.of(record));
            when(timeEntryRepository.findByTimeRecordIdOrderByClockIn(10L))
                    .thenReturn(List.of(closedEntry));

            assertThatThrownBy(() -> service.clockOut(employeeId))
                    .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("本日のレコードがない: エラー")
        void clockOut_noRecord_throwsException() {
            Long employeeId = 1L;
            when(timeRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.clockOut(employeeId))
                    .isInstanceOf(InvalidOperationException.class);
        }
    }

    @Nested
    @DisplayName("modifyEntry")
    class ModifyEntry {

        @Test
        @DisplayName("DRAFT状態: 修正成功")
        void modifyEntry_draftStatus_success() {
            var entry = TimeEntry.builder().id(1L).timeRecordId(10L)
                    .clockIn(LocalDateTime.of(2026, 7, 14, 9, 0))
                    .clockOut(LocalDateTime.of(2026, 7, 14, 17, 0))
                    .build();
            var record = TimeRecord.builder().id(10L).status(RecordStatus.DRAFT).build();

            when(timeEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
            when(timeRecordRepository.findById(10L)).thenReturn(Optional.of(record));
            when(timeEntryRepository.save(any(TimeEntry.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            var request = new ModifyEntryRequest(
                    LocalDateTime.of(2026, 7, 14, 9, 15),
                    LocalDateTime.of(2026, 7, 14, 18, 30)
            );

            var result = service.modifyEntry(1L, request);

            assertThat(result.clockIn()).isEqualTo(LocalDateTime.of(2026, 7, 14, 9, 15));
            assertThat(result.clockOut()).isEqualTo(LocalDateTime.of(2026, 7, 14, 18, 30));
        }

        @Test
        @DisplayName("APPROVED状態: 409エラー")
        void modifyEntry_approvedStatus_throwsConflict() {
            var entry = TimeEntry.builder().id(1L).timeRecordId(10L)
                    .clockIn(LocalDateTime.of(2026, 7, 14, 9, 0))
                    .build();
            var record = TimeRecord.builder().id(10L).status(RecordStatus.APPROVED).build();

            when(timeEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
            when(timeRecordRepository.findById(10L)).thenReturn(Optional.of(record));

            var request = new ModifyEntryRequest(
                    LocalDateTime.of(2026, 7, 14, 9, 15), null
            );

            assertThatThrownBy(() -> service.modifyEntry(1L, request))
                    .isInstanceOf(InvalidOperationException.class);
        }
    }

    @Nested
    @DisplayName("submitForApproval")
    class SubmitForApproval {

        @Test
        @DisplayName("DRAFT→SUBMITTED")
        void submit_draft_becomesSubmitted() {
            var record = TimeRecord.builder().id(10L).status(RecordStatus.DRAFT).build();
            when(timeRecordRepository.findById(10L)).thenReturn(Optional.of(record));
            when(timeRecordRepository.save(any(TimeRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.submitForApproval(10L);

            verify(timeRecordRepository).save(any(TimeRecord.class));
        }

        @Test
        @DisplayName("APPROVED状態で再提出: エラー")
        void submit_approved_throwsException() {
            var record = TimeRecord.builder().id(10L).status(RecordStatus.APPROVED).build();
            when(timeRecordRepository.findById(10L)).thenReturn(Optional.of(record));

            assertThatThrownBy(() -> service.submitForApproval(10L))
                    .isInstanceOf(InvalidOperationException.class);
        }
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("SUBMITTED→APPROVED")
        void approve_submitted_becomesApproved() {
            var record = TimeRecord.builder().id(10L).status(RecordStatus.SUBMITTED).build();
            when(timeRecordRepository.findById(10L)).thenReturn(Optional.of(record));
            when(timeRecordRepository.save(any(TimeRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.approve(10L, 2L);

            verify(timeRecordRepository).save(any(TimeRecord.class));
        }

        @Test
        @DisplayName("DRAFT状態で承認: エラー")
        void approve_draft_throwsException() {
            var record = TimeRecord.builder().id(10L).status(RecordStatus.DRAFT).build();
            when(timeRecordRepository.findById(10L)).thenReturn(Optional.of(record));

            assertThatThrownBy(() -> service.approve(10L, 2L))
                    .isInstanceOf(InvalidOperationException.class);
        }
    }
}
