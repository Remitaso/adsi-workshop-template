package com.example.attendance.service.impl;

import com.example.attendance.dto.CreateEmployeeRequest;
import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.dto.UpdateEmployeeRequest;
import com.example.attendance.entity.Employee;
import com.example.attendance.entity.Role;
import com.example.attendance.entity.Team;
import com.example.attendance.exception.DuplicateResourceException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.EmployeeRepository;
import com.example.attendance.repository.TeamRepository;
import com.example.attendance.service.EmployeeService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               TeamRepository teamRepository,
                               PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.teamRepository = teamRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("このメールアドレスは既に使用されています");
        }
        if (employeeRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new DuplicateResourceException("この社員コードは既に使用されています");
        }

        var now = LocalDateTime.now();
        var employee = Employee.builder()
                .employeeCode(request.employeeCode())
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.valueOf(request.role()))
                .teamId(request.teamId())
                .hireDate(request.hireDate())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        var saved = employeeRepository.save(employee);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        var employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません: " + id));

        employee.setName(request.name());
        employee.setEmail(request.email());
        employee.setRole(Role.valueOf(request.role()));
        employee.setTeamId(request.teamId());
        employee.setVersion(request.version());
        employee.setUpdatedAt(LocalDateTime.now());

        var saved = employeeRepository.save(employee);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        var employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません: " + id));
        employee.setActive(false);
        employee.setUpdatedAt(LocalDateTime.now());
        employeeRepository.save(employee);
    }

    @Override
    public EmployeeResponse findById(Long id) {
        var employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません: " + id));
        return toResponse(employee);
    }

    @Override
    public List<EmployeeResponse> findByTeamId(Long teamId) {
        return employeeRepository.findByTeamId(teamId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<EmployeeResponse> findAll(Long teamId, Boolean active) {
        List<Employee> employees;
        if (teamId != null) {
            employees = employeeRepository.findByTeamId(teamId);
        } else if (Boolean.TRUE.equals(active)) {
            employees = employeeRepository.findByActiveTrue();
        } else {
            employees = employeeRepository.findAll();
        }
        return employees.stream()
                .map(this::toResponse)
                .toList();
    }

    private EmployeeResponse toResponse(Employee employee) {
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
