package com.example.attendance.repository;

import com.example.attendance.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByDepartmentId(Long departmentId);
}
