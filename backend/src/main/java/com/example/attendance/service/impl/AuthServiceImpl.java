package com.example.attendance.service.impl;

import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.entity.Team;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.EmployeeRepository;
import com.example.attendance.repository.TeamRepository;
import com.example.attendance.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;

    public AuthServiceImpl(EmployeeRepository employeeRepository, TeamRepository teamRepository) {
        this.employeeRepository = employeeRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public EmployeeResponse getEmployeeByEmail(String email) {
        var employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません"));
        String teamName = teamRepository.findById(employee.getTeamId())
                .map(Team::getName)
                .orElse("");
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getName(),
                employee.getEmail(),
                employee.getRole().name(),
                teamName
        );
    }
}
