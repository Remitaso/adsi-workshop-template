package com.example.attendance.repository;

import com.example.attendance.entity.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByTimeRecordIdOrderByClockIn(Long timeRecordId);
}
