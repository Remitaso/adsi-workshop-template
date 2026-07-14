package com.example.attendance.service;

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
import com.example.attendance.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(employeeRepository, teamRepository, passwordEncoder);
    }

    @Test
    @DisplayName("社員登録: 正常に登録されレスポンスが返される")
    void create_validRequest_returnsEmployeeResponse() {
        var request = new CreateEmployeeRequest(
                "EMP100", "新入社員", "new@example.com", "password123",
                "EMPLOYEE", 1L, LocalDate.of(2026, 7, 1)
        );
        when(employeeRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(employeeRepository.existsByEmployeeCode("EMP100")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(
                Team.builder().id(1L).name("開発1チーム").build()
        ));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee e = invocation.getArgument(0);
            e.setId(100L);
            return e;
        });

        EmployeeResponse result = employeeService.create(request);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.name()).isEqualTo("新入社員");
        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.teamName()).isEqualTo("開発1チーム");
    }

    @Test
    @DisplayName("社員登録: メールアドレスが重複する場合はエラー")
    void create_duplicateEmail_throwsDuplicateException() {
        var request = new CreateEmployeeRequest(
                "EMP100", "新入社員", "existing@example.com", "password123",
                "EMPLOYEE", 1L, LocalDate.of(2026, 7, 1)
        );
        when(employeeRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("社員検索: 存在するIDで検索すると社員情報が返される")
    void findById_existingId_returnsEmployee() {
        var employee = Employee.builder()
                .id(1L).employeeCode("EMP001").name("田中太郎")
                .email("tanaka@example.com").role(Role.EMPLOYEE).teamId(1L)
                .build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(
                Team.builder().id(1L).name("開発1チーム").build()
        ));

        EmployeeResponse result = employeeService.findById(1L);

        assertThat(result.name()).isEqualTo("田中太郎");
        assertThat(result.teamName()).isEqualTo("開発1チーム");
    }

    @Test
    @DisplayName("社員検索: 存在しないIDで検索するとエラー")
    void findById_nonExistingId_throwsNotFound() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("社員無効化: activeがfalseに更新される")
    void deactivate_existingEmployee_setsActiveFalse() {
        var employee = Employee.builder()
                .id(1L).name("田中太郎").active(true)
                .build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        employeeService.deactivate(1L);

        assertThat(employee.getActive()).isFalse();
        verify(employeeRepository).save(employee);
    }
}
