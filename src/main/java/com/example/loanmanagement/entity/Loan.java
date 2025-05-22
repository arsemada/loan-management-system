package com.example.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal; // For precise monetary values
import java.time.LocalDateTime;

@Entity
@Table(name = "loans") // You can adjust table name if needed
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class) // For automatic createdAt/updatedAt
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ManyToOne relationship with User: One user can have many loans
    @ManyToOne(fetch = FetchType.LAZY) // Lazy fetching for performance
    @JoinColumn(name = "user_id", nullable = false) // Foreign key column
    private User user; // The user who applied for this loan

    @Column(nullable = false, precision = 19, scale = 2) // Standard for monetary values
    private BigDecimal amount;

    @Column(nullable = false, precision = 5, scale = 2) // For interest rate (e.g., 5.75%)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private Integer termMonths; // Loan term in months

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status; // Enum for loan status (PENDING, APPROVED, REJECTED, PAID)

    private String purpose; // Optional: e.g., "Home Improvement", "Education", "Debt Consolidation"

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime applicationDate; // When the loan was applied for

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastUpdated; // When the loan status or details were last modified

    // You might also want to add:
    // private LocalDateTime approvalDate;
    // private LocalDateTime repaymentStartDate;
    // private BigDecimal monthlyPayment;
    // ... depending on desired complexity

    // Enum for Loan Status
    public enum LoanStatus {
        PENDING,
        APPROVED,
        REJECTED,
        PAID
    }

    @PrePersist // Called before the entity is first saved (persisted)
    protected void onCreate() {
        if (applicationDate == null) {
            applicationDate = LocalDateTime.now();
        }
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
        if (status == null) {
            status = LoanStatus.PENDING; // Default status for new applications
        }
    }

    @PreUpdate // Called before the entity is updated
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}