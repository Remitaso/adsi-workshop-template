package com.example.attendance.leave.repository;

import com.example.attendance.leave.entity.ApprovalStatus;
import com.example.attendance.leave.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, ApprovalStatus status);

    List<LeaveRequest> findByEmployeeId(Long employeeId);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employeeId IN :employeeIds AND lr.status = :status")
    List<LeaveRequest> findByEmployeeIdInAndStatus(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("status") ApprovalStatus status);
}
