package com.example.attendance.service.impl;

import com.example.attendance.entity.Employee;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.repository.EmployeeRepository;
import com.example.attendance.repository.SectionRepository;
import com.example.attendance.repository.TeamRepository;
import com.example.attendance.service.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional(readOnly = true)
public class OrganizationServiceImpl implements OrganizationService {

    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;

    public OrganizationServiceImpl(DepartmentRepository departmentRepository,
                                   SectionRepository sectionRepository,
                                   TeamRepository teamRepository,
                                   EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.teamRepository = teamRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Map<String, Object> getDepartmentsHierarchy() {
        var departments = departmentRepository.findAll().stream()
                .map(dept -> {
                    var sections = sectionRepository.findByDepartmentId(dept.getId()).stream()
                            .map(section -> {
                                var teams = teamRepository.findBySectionId(section.getId()).stream()
                                        .map(team -> {
                                            String managerName = team.getManagerId() != null
                                                    ? employeeRepository.findById(team.getManagerId())
                                                    .map(Employee::getName).orElse("")
                                                    : "";
                                            return Map.of(
                                                    "id", (Object) team.getId(),
                                                    "name", (Object) team.getName(),
                                                    "managerName", (Object) managerName
                                            );
                                        })
                                        .toList();
                                return Map.of(
                                        "id", (Object) section.getId(),
                                        "name", (Object) section.getName(),
                                        "teams", (Object) teams
                                );
                            })
                            .toList();
                    return Map.of(
                            "id", (Object) dept.getId(),
                            "name", (Object) dept.getName(),
                            "sections", (Object) sections
                    );
                })
                .toList();
        return Map.of("departments", departments);
    }
}
