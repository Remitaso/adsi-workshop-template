package com.example.attendance.leave.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"employee_id", "grant_date"})
})
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "grant_date", nullable = false)
    private LocalDate grantDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "granted_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal grantedDays;

    @Column(name = "used_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal usedDays;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LeaveBalance() {
    }

    public LeaveBalance(Long employeeId, LocalDate grantDate, BigDecimal grantedDays) {
        this.employeeId = employeeId;
        this.grantDate = grantDate;
        this.expiryDate = grantDate.plusYears(2);
        this.grantedDays = grantedDays;
        this.usedDays = BigDecimal.ZERO;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getRemaining() {
        return grantedDays.subtract(usedDays);
    }

    public boolean isExpired(LocalDate asOf) {
        return asOf.isAfter(expiryDate) || asOf.isEqual(expiryDate);
    }

    public boolean hasRemaining() {
        return getRemaining().compareTo(BigDecimal.ZERO) > 0;
    }

    public void deduct(BigDecimal days) {
        if (days.compareTo(getRemaining()) > 0) {
            throw new IllegalArgumentException("Cannot deduct more than remaining balance");
        }
        this.usedDays = this.usedDays.add(days);
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public LocalDate getGrantDate() { return grantDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public BigDecimal getGrantedDays() { return grantedDays; }
    public BigDecimal getUsedDays() { return usedDays; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
