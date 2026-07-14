package com.example.attendance.service.impl;

import com.example.attendance.domain.WorkDuration;
import com.example.attendance.dto.ModifyEntryRequest;
import com.example.attendance.dto.MonthlyRecordsResponse;
import com.example.attendance.dto.TimeEntryResponse;
import com.example.attendance.dto.TodayAttendanceResponse;
import com.example.attendance.entity.RecordStatus;
import com.example.attendance.entity.TimeEntry;
import com.example.attendance.entity.TimeRecord;
import com.example.attendance.exception.InvalidOperationException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.TimeEntryRepository;
import com.example.attendance.repository.TimeRecordRepository;
import com.example.attendance.service.AttendanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final TimeRecordRepository timeRecordRepository;
    private final TimeEntryRepository timeEntryRepository;

    public AttendanceServiceImpl(TimeRecordRepository timeRecordRepository,
                                 TimeEntryRepository timeEntryRepository) {
        this.timeRecordRepository = timeRecordRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    @Override
    public TimeEntryResponse clockIn(Long employeeId) {
        LocalDate today = LocalDate.now();
        TimeRecord record = timeRecordRepository.findByEmployeeIdAndWorkDate(employeeId, today)
                .orElseGet(() -> createNewRecord(employeeId, today));

        List<TimeEntry> entries = timeEntryRepository.findByTimeRecordIdOrderByClockIn(record.getId());
        boolean hasOpenEntry = entries.stream().anyMatch(e -> e.getClockOut() == null);
        if (hasOpenEntry) {
            throw new InvalidOperationException("既に出勤中です。退勤してから再度出勤してください。");
        }

        TimeEntry entry = TimeEntry.builder()
                .timeRecordId(record.getId())
                .clockIn(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        entry = timeEntryRepository.save(entry);

        return TimeEntryResponse.from(entry);
    }

    @Override
    public TimeEntryResponse clockOut(Long employeeId) {
        LocalDate today = LocalDate.now();
        TimeRecord record = timeRecordRepository.findByEmployeeIdAndWorkDate(employeeId, today)
                .orElseThrow(() -> new InvalidOperationException("本日の出勤記録がありません。"));

        List<TimeEntry> entries = timeEntryRepository.findByTimeRecordIdOrderByClockIn(record.getId());
        TimeEntry openEntry = entries.stream()
                .filter(e -> e.getClockOut() == null)
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException("出勤中の記録がありません。"));

        openEntry.setClockOut(LocalDateTime.now());
        openEntry.setUpdatedAt(LocalDateTime.now());
        openEntry = timeEntryRepository.save(openEntry);

        return TimeEntryResponse.from(openEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public TodayAttendanceResponse getToday(Long employeeId) {
        LocalDate today = LocalDate.now();
        return timeRecordRepository.findByEmployeeIdAndWorkDate(employeeId, today)
                .map(record -> {
                    List<TimeEntry> entries = timeEntryRepository.findByTimeRecordIdOrderByClockIn(record.getId());
                    WorkDuration duration = WorkDuration.calculate(entries);
                    List<TodayAttendanceResponse.EntryDto> entryDtos = entries.stream()
                            .map(e -> new TodayAttendanceResponse.EntryDto(
                                    e.getId(),
                                    e.getClockIn().toString(),
                                    e.getClockOut() != null ? e.getClockOut().toString() : null
                            ))
                            .toList();
                    return new TodayAttendanceResponse(
                            record.getWorkDate(),
                            record.getStatus().name(),
                            entryDtos,
                            duration.totalMinutes()
                    );
                })
                .orElse(new TodayAttendanceResponse(today, null, List.of(), 0));
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyRecordsResponse getMonthlyRecords(Long employeeId, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<TimeRecord> records = timeRecordRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDate(employeeId, startDate, endDate);

        List<MonthlyRecordsResponse.DailyRecord> dailyRecords = records.stream()
                .map(record -> {
                    List<TimeEntry> entries = timeEntryRepository.findByTimeRecordIdOrderByClockIn(record.getId());
                    WorkDuration duration = WorkDuration.calculate(entries);
                    List<TodayAttendanceResponse.EntryDto> entryDtos = entries.stream()
                            .map(e -> new TodayAttendanceResponse.EntryDto(
                                    e.getId(),
                                    e.getClockIn().toString(),
                                    e.getClockOut() != null ? e.getClockOut().toString() : null
                            ))
                            .toList();
                    return new MonthlyRecordsResponse.DailyRecord(
                            record.getWorkDate().toString(),
                            record.getStatus().name(),
                            duration.totalMinutes(),
                            duration.overtimeMinutes(),
                            entryDtos
                    );
                })
                .toList();

        return new MonthlyRecordsResponse(yearMonth.toString(), dailyRecords);
    }

    @Override
    public TimeEntryResponse modifyEntry(Long employeeId, Long entryId, ModifyEntryRequest request) {
        TimeEntry entry = timeEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("打刻エントリが見つかりません: " + entryId));

        TimeRecord record = timeRecordRepository.findById(entry.getTimeRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("勤怠記録が見つかりません"));

        if (!record.getEmployeeId().equals(employeeId)) {
            throw new InvalidOperationException("他のユーザーの打刻記録は修正できません。");
        }

        if (record.getStatus() != RecordStatus.DRAFT) {
            throw new InvalidOperationException("承認済みまたは申請中の記録は修正できません。");
        }

        if (request.clockOut() != null && request.clockOut().isBefore(request.clockIn())) {
            throw new InvalidOperationException("退勤時刻は出勤時刻より後でなければなりません。");
        }

        entry.setClockIn(request.clockIn());
        entry.setClockOut(request.clockOut());
        entry.setUpdatedAt(LocalDateTime.now());
        entry = timeEntryRepository.save(entry);

        return TimeEntryResponse.from(entry);
    }

    @Override
    public void submitForApproval(Long employeeId, Long timeRecordId) {
        TimeRecord record = timeRecordRepository.findById(timeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("勤怠記録が見つかりません: " + timeRecordId));

        if (!record.getEmployeeId().equals(employeeId)) {
            throw new InvalidOperationException("他のユーザーの勤怠記録は申請できません。");
        }

        if (record.getStatus() != RecordStatus.DRAFT) {
            throw new InvalidOperationException("DRAFT状態の記録のみ申請できます。");
        }

        record.setStatus(RecordStatus.SUBMITTED);
        record.setUpdatedAt(LocalDateTime.now());
        timeRecordRepository.save(record);
    }

    @Override
    public void approve(Long timeRecordId, Long approverId) {
        TimeRecord record = timeRecordRepository.findById(timeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("勤怠記録が見つかりません: " + timeRecordId));

        if (record.getStatus() != RecordStatus.SUBMITTED) {
            throw new InvalidOperationException("SUBMITTED状態の記録のみ承認できます。");
        }

        record.setStatus(RecordStatus.APPROVED);
        record.setApprovedBy(approverId);
        record.setApprovedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        timeRecordRepository.save(record);
    }

    private TimeRecord createNewRecord(Long employeeId, LocalDate workDate) {
        TimeRecord record = TimeRecord.builder()
                .employeeId(employeeId)
                .workDate(workDate)
                .status(RecordStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return timeRecordRepository.save(record);
    }
}
