package com.example.attendance.leave.service;

import com.example.attendance.entity.Employee;
import com.example.attendance.entity.Role;
import com.example.attendance.entity.Team;
import com.example.attendance.leave.entity.ApprovalStatus;
import com.example.attendance.leave.entity.LeaveBalance;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveType;
import com.example.attendance.leave.exception.InsufficientLeaveBalanceException;
import com.example.attendance.leave.repository.LeaveBalanceRepository;
import com.example.attendance.leave.repository.LeaveRequestRepository;
import com.example.attendance.leave.service.impl.LeaveServiceImpl;
import com.example.attendance.repository.EmployeeRepository;
import com.example.attendance.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private TeamRepository teamRepository;

    private LeaveServiceImpl leaveService;

    @BeforeEach
    void setUp() {
        leaveService = new LeaveServiceImpl(
                leaveRequestRepository, leaveBalanceRepository,
                employeeRepository, teamRepository);
    }

    @Test
    @DisplayName("有給申請: 残高あり → PENDINGで作成される")
    void apply_paid_sufficientBalance_createsRequest() {
        // Arrange
        Long employeeId = 1L;
        LocalDate start = LocalDate.of(2026, 7, 20);
        LocalDate end = LocalDate.of(2026, 7, 20);
        LeaveBalance balance = new LeaveBalance(employeeId, LocalDate.of(2026, 4, 1), BigDecimal.TEN);

        when(leaveBalanceRepository.findValidBalances(eq(employeeId), any(LocalDate.class)))
                .thenReturn(List.of(balance));
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LeaveRequest result = leaveService.apply(employeeId, LeaveType.PAID, start, end, "私用");

        // Assert
        assertThat(result.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(result.getLeaveType()).isEqualTo(LeaveType.PAID);
        assertThat(result.getEmployeeId()).isEqualTo(employeeId);
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("有給申請: 残高不足 → InsufficientLeaveBalanceException")
    void apply_paid_insufficientBalance_throwsException() {
        // Arrange
        Long employeeId = 1L;
        LocalDate start = LocalDate.of(2026, 7, 20);
        LocalDate end = LocalDate.of(2026, 7, 25);

        when(leaveBalanceRepository.findValidBalances(eq(employeeId), any(LocalDate.class)))
                .thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> leaveService.apply(employeeId, LeaveType.PAID, start, end, "旅行"))
                .isInstanceOf(InsufficientLeaveBalanceException.class);
    }

    @Test
    @DisplayName("半休申請: 0.5日の残高チェック")
    void apply_halfDay_consumesHalfDay() {
        // Arrange
        Long employeeId = 1L;
        LocalDate date = LocalDate.of(2026, 7, 20);
        LeaveBalance balance = new LeaveBalance(employeeId, LocalDate.of(2026, 4, 1), BigDecimal.ONE);

        when(leaveBalanceRepository.findValidBalances(eq(employeeId), any(LocalDate.class)))
                .thenReturn(List.of(balance));
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LeaveRequest result = leaveService.apply(employeeId, LeaveType.HALF_DAY, date, date, "通院");

        // Assert
        assertThat(result.getLeaveType()).isEqualTo(LeaveType.HALF_DAY);
        assertThat(result.getStartDate()).isEqualTo(result.getEndDate());
    }

    @Test
    @DisplayName("特別休暇申請: 残高チェックなし")
    void apply_special_noBalanceCheck() {
        // Arrange
        Long employeeId = 1L;
        LocalDate start = LocalDate.of(2026, 7, 20);
        LocalDate end = LocalDate.of(2026, 7, 22);

        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LeaveRequest result = leaveService.apply(employeeId, LeaveType.SPECIAL, start, end, "慶弔");

        // Assert
        assertThat(result.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        verify(leaveBalanceRepository, never()).findValidBalances(any(), any());
    }

    @Test
    @DisplayName("承認: PENDING → APPROVED + 古い残高から消化")
    void approve_pendingRequest_approvesAndDeductsBalance() {
        // Arrange
        Long requestId = 1L;
        Long managerId = 2L;
        Long employeeId = 3L;

        LeaveRequest request = new LeaveRequest(employeeId, LeaveType.PAID,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 21), "私用");

        Employee employee = new Employee();
        employee.setTeamId(10L);

        Team team = new Team();
        team.setManagerId(managerId);

        // 古い残高: 付与3日、新しい残高: 付与10日 → 2日申請は古い残高から全消化
        LeaveBalance oldBalance = new LeaveBalance(employeeId, LocalDate.of(2025, 4, 1), new BigDecimal("3.0"));
        LeaveBalance newBalance = new LeaveBalance(employeeId, LocalDate.of(2026, 4, 1), BigDecimal.TEN);

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(leaveBalanceRepository.findValidBalances(eq(employeeId), any(LocalDate.class)))
                .thenReturn(List.of(oldBalance, newBalance));

        // Act
        LeaveRequest result = leaveService.approve(requestId, managerId);

        // Assert
        assertThat(result.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(oldBalance.getUsedDays()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(newBalance.getUsedDays()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("承認: チームのmanagerでない → 例外")
    void approve_notManager_throwsForbidden() {
        // Arrange
        Long requestId = 1L;
        Long notManagerId = 99L;
        Long employeeId = 3L;

        LeaveRequest request = new LeaveRequest(employeeId, LeaveType.PAID,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20), "私用");

        Employee employee = new Employee();
        employee.setTeamId(10L);

        Team team = new Team();
        team.setManagerId(2L);

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        // Act & Assert
        assertThatThrownBy(() -> leaveService.approve(requestId, notManagerId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("却下: PENDING → REJECTED、残高変動なし")
    void reject_pendingRequest_rejectsWithReason() {
        // Arrange
        Long requestId = 1L;
        Long managerId = 2L;
        Long employeeId = 3L;

        LeaveRequest request = new LeaveRequest(employeeId, LeaveType.PAID,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20), "私用");

        Employee employee = new Employee();
        employee.setTeamId(10L);

        Team team = new Team();
        team.setManagerId(managerId);

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        // Act
        LeaveRequest result = leaveService.reject(requestId, managerId, "業務都合");

        // Assert
        assertThat(result.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(result.getRejectReason()).isEqualTo("業務都合");
        verify(leaveBalanceRepository, never()).findValidBalances(any(), any());
    }

    @Test
    @DisplayName("取消: PENDING → 削除成功")
    void cancel_pendingRequest_deletesRequest() {
        // Arrange
        Long requestId = 1L;
        Long employeeId = 1L;

        LeaveRequest request = new LeaveRequest(employeeId, LeaveType.PAID,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20), "私用");

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // Act
        leaveService.cancel(requestId, employeeId);

        // Assert
        verify(leaveRequestRepository).delete(request);
    }

    @Test
    @DisplayName("取消: 承認済み → 例外")
    void cancel_approvedRequest_throwsException() {
        // Arrange
        Long requestId = 1L;
        Long employeeId = 1L;

        LeaveRequest request = new LeaveRequest(employeeId, LeaveType.PAID,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20), "私用");
        request.approve(2L);

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        assertThatThrownBy(() -> leaveService.cancel(requestId, employeeId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("残高照会: 有効期限内の残高のみ返す")
    void getBalance_returnsValidBalancesOnly() {
        // Arrange
        Long employeeId = 1L;
        LeaveBalance valid = new LeaveBalance(employeeId, LocalDate.of(2025, 10, 1), new BigDecimal("15.0"));
        valid.deduct(new BigDecimal("5.0"));

        when(leaveBalanceRepository.findValidBalances(eq(employeeId), any(LocalDate.class)))
                .thenReturn(List.of(valid));

        // Act
        var result = leaveService.getBalance(employeeId);

        // Assert
        assertThat(result.totalRemaining()).isEqualTo(new BigDecimal("10.0"));
        assertThat(result.details()).hasSize(1);
    }

    @Test
    @DisplayName("有給付与: 新入社員 → 10日付与")
    void grantAnnualLeave_newHire_grants10Days() {
        // Arrange
        Long employeeId = 1L;
        Employee employee = new Employee();
        employee.setHireDate(LocalDate.of(2026, 4, 1));

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(leaveBalanceRepository.save(any(LeaveBalance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LeaveBalance result = leaveService.grantAnnualLeave(employeeId, LocalDate.of(2026, 4, 1));

        // Assert
        assertThat(result.getGrantedDays()).isEqualTo(new BigDecimal("10"));
        assertThat(result.getGrantDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(result.getExpiryDate()).isEqualTo(LocalDate.of(2028, 4, 1));
    }

    @Test
    @DisplayName("有給付与: 1年6ヶ月 → 11日")
    void grantAnnualLeave_1year6months_grants11Days() {
        // Arrange
        Long employeeId = 1L;
        Employee employee = new Employee();
        employee.setHireDate(LocalDate.of(2025, 4, 1));

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(leaveBalanceRepository.save(any(LeaveBalance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act - 付与日は入社1年6ヶ月後の応当日
        LeaveBalance result = leaveService.grantAnnualLeave(employeeId, LocalDate.of(2026, 10, 1));

        // Assert
        assertThat(result.getGrantedDays()).isEqualTo(new BigDecimal("11"));
    }

    @Test
    @DisplayName("有給付与: 6年6ヶ月以上 → 20日")
    void grantAnnualLeave_6year6months_grants20Days() {
        // Arrange
        Long employeeId = 1L;
        Employee employee = new Employee();
        employee.setHireDate(LocalDate.of(2019, 4, 1));

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(leaveBalanceRepository.save(any(LeaveBalance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act - 付与日は入社7年6ヶ月後
        LeaveBalance result = leaveService.grantAnnualLeave(employeeId, LocalDate.of(2026, 10, 1));

        // Assert
        assertThat(result.getGrantedDays()).isEqualTo(new BigDecimal("20"));
    }

    @Test
    @DisplayName("残高消化: 古い年度から先に消化される")
    void deductBalance_consumesOldestFirst() {
        // Arrange
        Long employeeId = 3L;
        Long managerId = 2L;
        Long requestId = 1L;

        // 3日間の有給申請
        LeaveRequest request = new LeaveRequest(employeeId, LeaveType.PAID,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 22), "旅行");

        Employee employee = new Employee();
        employee.setTeamId(10L);

        Team team = new Team();
        team.setManagerId(managerId);

        // 古い残高: 残り2日、新しい残高: 残り10日
        LeaveBalance oldBalance = new LeaveBalance(employeeId, LocalDate.of(2025, 4, 1), new BigDecimal("10.0"));
        oldBalance.deduct(new BigDecimal("8.0"));
        LeaveBalance newBalance = new LeaveBalance(employeeId, LocalDate.of(2026, 4, 1), BigDecimal.TEN);

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(leaveBalanceRepository.findValidBalances(eq(employeeId), any(LocalDate.class)))
                .thenReturn(List.of(oldBalance, newBalance));

        // Act
        leaveService.approve(requestId, managerId);

        // Assert - 古い残高から消化: 2日 → 0日、新しい残高: 1日消化
        assertThat(oldBalance.getUsedDays()).isEqualTo(new BigDecimal("10.0"));
        assertThat(newBalance.getUsedDays()).isEqualTo(new BigDecimal("1.0"));
    }
}
