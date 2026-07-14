package com.example.attendance.leave.repository;

import com.example.attendance.leave.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.employeeId = :employeeId " +
           "AND lb.expiryDate > :asOf ORDER BY lb.grantDate ASC")
    List<LeaveBalance> findValidBalances(
            @Param("employeeId") Long employeeId,
            @Param("asOf") LocalDate asOf);

    List<LeaveBalance> findByEmployeeIdOrderByGrantDateAsc(Long employeeId);
}
