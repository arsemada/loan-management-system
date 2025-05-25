package com.example.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;


    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal loanAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanType loanType;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "purpose", nullable = false)
    private String purpose;

    @Column(name = "annual_income", nullable = false, precision = 19, scale = 2)
    private BigDecimal annualIncome;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "rejected_reason")
    private String rejectedReason;

    @Column(name = "approved_amount", precision = 19, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "loan_start_date")
    private LocalDateTime loanStartDate;

    @Column(name = "monthly_emi", precision = 19, scale = 2)
    private BigDecimal monthlyEMI;

    // --- Audit Fields ---
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime applicationDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastUpdated;



    public enum LoanStatus {
        PENDING,
        APPROVED,
        REJECTED,
        DISBURSED,
        PAID,
        OVERDUE
    }
}