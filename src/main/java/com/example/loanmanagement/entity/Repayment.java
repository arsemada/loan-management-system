package com.example.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate; // Import for CreatedDate
import org.springframework.data.annotation.LastModifiedDate; // Import for LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener; // Import for AuditingEntityListener

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repayments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class) // <--- ADD THIS LINE
public class Repayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_payment", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPayment;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepaymentStatus status;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    // --- Audit Fields for Repayment ---
    @CreatedDate // <--- ADD THIS LINE
    @Column(nullable = false, updatable = false) // <--- ADD THIS LINE
    private LocalDateTime createdDate; // <--- ADD THIS FIELD

    @LastModifiedDate // <--- ADD THIS LINE
    @Column(nullable = false) // <--- ADD THIS LINE
    private LocalDateTime lastUpdated; // <--- ADD THIS FIELD

    public enum RepaymentStatus {
        PENDING,
        PAID,
        OVERDUE
    }
}
