package com.example.attendance.service;

import com.example.attendance.dto.EmployeeResponse;

public interface AuthService {

    EmployeeResponse getEmployeeByEmail(String email);
}
