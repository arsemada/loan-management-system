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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "monthly_payment", precision = 19, scale = 2)
    private BigDecimal monthlyPayment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(name = "purpose")
    private String purpose;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime applicationDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastUpdated;


    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "rejected_reason")
    private String rejectedReason;


    public enum LoanStatus {
        PENDING,
        APPROVED,
        REJECTED,
        PAID
    }

}