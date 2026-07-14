package com.example.attendance.repository;

import com.example.attendance.entity.TimeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimeRecordRepository extends JpaRepository<TimeRecord, Long> {

    Optional<TimeRecord> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    List<TimeRecord> findByEmployeeIdAndWorkDateBetweenOrderByWorkDate(
            Long employeeId, LocalDate startDate, LocalDate endDate);
}
