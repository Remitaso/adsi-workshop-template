package com.example.attendance.leave.service.impl;

import com.example.attendance.entity.Employee;
import com.example.attendance.entity.Team;
import com.example.attendance.leave.dto.LeaveBalanceDetailResponse;
import com.example.attendance.leave.dto.LeaveBalanceResponse;
import com.example.attendance.leave.entity.ApprovalStatus;
import com.example.attendance.leave.entity.LeaveBalance;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveType;
import com.example.attendance.leave.exception.InsufficientLeaveBalanceException;
import com.example.attendance.leave.repository.LeaveBalanceRepository;
import com.example.attendance.leave.repository.LeaveRequestRepository;
import com.example.attendance.leave.service.LeaveService;
import com.example.attendance.repository.EmployeeRepository;
import com.example.attendance.repository.TeamRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;

    public LeaveServiceImpl(LeaveRequestRepository leaveRequestRepository,
                            LeaveBalanceRepository leaveBalanceRepository,
                            EmployeeRepository employeeRepository,
                            TeamRepository teamRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public LeaveRequest apply(Long employeeId, LeaveType leaveType,
                              LocalDate startDate, LocalDate endDate, String reason) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("開始日は終了日以前である必要があります");
        }
        if (leaveType == LeaveType.HALF_DAY && !startDate.equals(endDate)) {
            throw new IllegalArgumentException("半休は1日のみ指定可能です");
        }

        BigDecimal requiredDays = calculateLeaveDays(leaveType, startDate, endDate);

        if (requiresBalanceCheck(leaveType)) {
            BigDecimal totalRemaining = getTotalRemaining(employeeId);
            if (totalRemaining.compareTo(requiredDays) < 0) {
                throw new InsufficientLeaveBalanceException(
                        "有給残日数が不足しています（残: " + totalRemaining + "日, 必要: " + requiredDays + "日）");
            }
        }

        LeaveRequest request = new LeaveRequest(employeeId, leaveType, startDate, endDate, reason);
        return leaveRequestRepository.save(request);
    }

    @Override
    public void cancel(Long requestId, Long employeeId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("申請が見つかりません"));

        if (!request.getEmployeeId().equals(employeeId)) {
            throw new AccessDeniedException("他者の申請は取消できません");
        }
        if (!request.isPending()) {
            throw new IllegalStateException("承認済みまたは却下済みの申請は取消できません");
        }

        leaveRequestRepository.delete(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequest> findByEmployee(Long employeeId, String status) {
        if (status != null && !status.isEmpty()) {
            return leaveRequestRepository.findByEmployeeIdAndStatus(
                    employeeId, ApprovalStatus.valueOf(status));
        }
        return leaveRequestRepository.findByEmployeeId(employeeId);
    }

    @Override
    public LeaveRequest approve(Long requestId, Long approverId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("申請が見つかりません"));

        if (!request.isPending()) {
            throw new IllegalStateException("承認待ち状態ではありません");
        }

        verifyManagerAuthority(request.getEmployeeId(), approverId);

        if (requiresBalanceCheck(request.getLeaveType())) {
            BigDecimal days = calculateLeaveDays(
                    request.getLeaveType(), request.getStartDate(), request.getEndDate());
            deductBalance(request.getEmployeeId(), days);
        }

        request.approve(approverId);
        return request;
    }

    @Override
    public LeaveRequest reject(Long requestId, Long approverId, String reason) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("申請が見つかりません"));

        if (!request.isPending()) {
            throw new IllegalStateException("承認待ち状態ではありません");
        }

        verifyManagerAuthority(request.getEmployeeId(), approverId);

        request.reject(approverId, reason);
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveBalanceResponse getBalance(Long employeeId) {
        List<LeaveBalance> balances = leaveBalanceRepository.findValidBalances(
                employeeId, LocalDate.now());

        List<LeaveBalanceDetailResponse> details = balances.stream()
                .map(LeaveBalanceDetailResponse::from)
                .toList();

        BigDecimal totalRemaining = balances.stream()
                .map(LeaveBalance::getRemaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LeaveBalanceResponse(totalRemaining, details);
    }

    @Override
    public LeaveBalance grantAnnualLeave(Long employeeId, LocalDate grantDate) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("社員が見つかりません"));

        BigDecimal days = calculateGrantDays(employee.getHireDate(), grantDate);
        LeaveBalance balance = new LeaveBalance(employeeId, grantDate, days);
        return leaveBalanceRepository.save(balance);
    }

    @Override
    public BigDecimal calculateLeaveDays(LeaveType leaveType, LocalDate startDate, LocalDate endDate) {
        if (leaveType == LeaveType.HALF_DAY) {
            return new BigDecimal("0.5");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return BigDecimal.valueOf(days);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequest> findPendingByManager(Long managerId) {
        List<Team> teams = teamRepository.findByManagerId(managerId);
        if (teams.isEmpty()) {
            return List.of();
        }

        List<Long> teamIds = teams.stream().map(Team::getId).toList();
        List<Employee> employees = employeeRepository.findByTeamIdIn(teamIds);
        List<Long> employeeIds = employees.stream().map(Employee::getId).toList();

        if (employeeIds.isEmpty()) {
            return List.of();
        }
        return leaveRequestRepository.findByEmployeeIdInAndStatus(employeeIds, ApprovalStatus.PENDING);
    }

    private boolean requiresBalanceCheck(LeaveType leaveType) {
        return leaveType == LeaveType.PAID || leaveType == LeaveType.HALF_DAY;
    }

    private BigDecimal getTotalRemaining(Long employeeId) {
        List<LeaveBalance> balances = leaveBalanceRepository.findValidBalances(
                employeeId, LocalDate.now());
        return balances.stream()
                .map(LeaveBalance::getRemaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void deductBalance(Long employeeId, BigDecimal days) {
        List<LeaveBalance> balances = leaveBalanceRepository.findValidBalances(
                employeeId, LocalDate.now());

        BigDecimal remaining = days;
        for (LeaveBalance balance : balances) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal available = balance.getRemaining();
            if (available.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal toDeduct = remaining.min(available);
            balance.deduct(toDeduct);
            remaining = remaining.subtract(toDeduct);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new InsufficientLeaveBalanceException("残高不足で消化できません");
        }
    }

    private void verifyManagerAuthority(Long employeeId, Long approverId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("社員が見つかりません"));

        Team team = teamRepository.findById(employee.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("チームが見つかりません"));

        if (!team.getManagerId().equals(approverId)) {
            throw new AccessDeniedException("この申請の承認権限がありません");
        }
    }

    BigDecimal calculateGrantDays(LocalDate hireDate, LocalDate grantDate) {
        long monthsBetween = ChronoUnit.MONTHS.between(hireDate, grantDate);

        if (monthsBetween < 6) return new BigDecimal("10");
        if (monthsBetween < 18) return new BigDecimal("10");
        if (monthsBetween < 30) return new BigDecimal("11");
        if (monthsBetween < 42) return new BigDecimal("12");
        if (monthsBetween < 54) return new BigDecimal("14");
        if (monthsBetween < 66) return new BigDecimal("16");
        if (monthsBetween < 78) return new BigDecimal("18");
        return new BigDecimal("20");
    }
}
