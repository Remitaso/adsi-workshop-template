package com.example.attendance.dto;

public record EmployeeResponse(
        Long id,
        String employeeCode,
        String name,
        String email,
        String role,
        String teamName
) {}
