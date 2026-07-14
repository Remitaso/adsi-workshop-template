package com.example.attendance.service;

import com.example.attendance.dto.CreateEmployeeRequest;
import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.dto.UpdateEmployeeRequest;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse create(CreateEmployeeRequest request);

    EmployeeResponse update(Long id, UpdateEmployeeRequest request);

    void deactivate(Long id);

    EmployeeResponse findById(Long id);

    List<EmployeeResponse> findByTeamId(Long teamId);

    List<EmployeeResponse> findAll(Long teamId, Boolean active);
}
