package com.example.attendance.service;

import com.example.attendance.repository.EmployeeRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class EmployeeIdResolver {

    private final EmployeeRepository employeeRepository;

    public EmployeeIdResolver(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Long resolve(UserDetails user) {
        return employeeRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new IllegalStateException("認証済みユーザーが見つかりません"))
                .getId();
    }
}
