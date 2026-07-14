package com.example.attendance.repository;

import com.example.attendance.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findBySectionId(Long sectionId);
}
