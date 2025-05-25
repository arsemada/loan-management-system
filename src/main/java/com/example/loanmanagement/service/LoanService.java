package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.LoanApplicationRequest;
import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.entity.Repayment;
import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.repository.LoanRepository;
import com.example.loanmanagement.repository.RepaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;

    /**
     * Allows a customer to apply for a loan.
     * Initial status is PENDING. EMI and schedule are NOT generated here.
     * They are generated upon admin approval.
     */
    @Transactional
    public Loan applyForLoan(LoanApplicationRequest request, User customer) {
        log.info("Attempting to apply for loan for user: {} with loanAmount: {}, durationMonths: {}, purpose: {}, annualIncome: {}",
                customer.getUsername(), request.getLoanAmount(), request.getDurationMonths(), request.getPurpose(), request.getAnnualIncome());

        Loan newLoan = Loan.builder()
                .customer(customer)
                .loanAmount(request.getLoanAmount())
                .loanType(request.getLoanType())
                .durationMonths(request.getDurationMonths())
                .purpose(request.getPurpose())
                .annualIncome(request.getAnnualIncome())
                .status(Loan.LoanStatus.PENDING)
                .build();

        Loan savedLoan = loanRepository.save(newLoan);
        log.info("Loan application saved successfully with ID: {}", savedLoan.getId());

        return savedLoan;
    }

    @Transactional(readOnly = true)
    public List<Loan> getLoansByCustomer(User customer) {
        log.info("Fetching loans for customer: {}", customer.getUsername());
        return loanRepository.findByCustomer(customer);
    }

    @Transactional(readOnly = true)
    public Optional<Loan> getLoanById(Long loanId) {
        log.info("Fetching loan by ID: {}", loanId);
        return loanRepository.findById(loanId);
    }

    @Transactional(readOnly = true)
    public List<Repayment> getRepaymentsForLoan(Loan loan) {
        log.info("Fetching repayments for loan ID: {}", loan.getId());
        return repaymentRepository.findByLoanOrderByDueDateAsc(loan);
    }

    /**
     * Calculates the Equated Monthly Installment (EMI) for a loan.
     * EMI = P * R * (1 + R)^N / ((1 + R)^N - 1)
     * Where:
     * P = loan amount (principal)
     * R = monthly interest rate (annual interest rate / 1200)
     * N = tenure (months)
     */
    public BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualInterestRate, Integer durationMonths) {
        log.debug("Calculating EMI for principal: {}, annualInterestRate: {}, durationMonths: {}", principal, annualInterestRate, durationMonths);

        // Ensure parameters are not null
        if (principal == null || annualInterestRate == null || durationMonths == null || durationMonths <= 0) {
            log.error("Invalid input for EMI calculation: principal={}, annualInterestRate={}, durationMonths={}", principal, annualInterestRate, durationMonths);
            throw new IllegalArgumentException("Principal, annual interest rate, and duration months must be valid for EMI calculation.");
        }

        // Monthly interest rate in decimal form (e.g., 8% annual becomes 0.08/12 monthly)
        BigDecimal monthlyInterestRate = annualInterestRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP) // e.g., 5.0 -> 0.05
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP); // e.g., 0.05 -> 0.00416667
        log.debug("Calculated monthly interest rate: {}", monthlyInterestRate);

        // Handle zero interest rate case (simple division)
        if (monthlyInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Monthly interest rate is zero. Using simple division for EMI.");
            return principal.divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.HALF_UP);
        }

        // (1 + R)^N
        BigDecimal powFactor = monthlyInterestRate.add(BigDecimal.ONE).pow(durationMonths);
        log.debug("Calculated powFactor (1+R)^N: {}", powFactor);

        // Numerator: P * R * (1 + R)^N
        BigDecimal numerator = principal.multiply(monthlyInterestRate).multiply(powFactor);
        log.debug("Calculated numerator: {}", numerator);

        // Denominator: (1 + R)^N - 1
        BigDecimal denominator = powFactor.subtract(BigDecimal.ONE);
        log.debug("Calculated denominator: {}", denominator);

        // Handle zero denominator (shouldn't happen with positive interest rate and term)
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            log.error("Denominator for EMI calculation is zero. This indicates a mathematical issue. Principal: {}, annualInterestRate: {}, durationMonths: {}",
                    principal, annualInterestRate, durationMonths);
            // Fallback to simple division, though this indicates an invalid input or edge case
            return principal.divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal emi = numerator.divide(denominator, 2, RoundingMode.HALF_UP); // Scale to 2 decimal places for currency
        log.debug("Calculated EMI: {}", emi);
        return emi;
    }

    /**
     * Generates and saves the repayment schedule for an approved loan.
     */
    @Transactional
    public void generateRepaymentSchedule(Loan loan) {
        log.info("Starting repayment schedule generation for Loan ID: {}", loan.getId());
        log.debug("Loan details for schedule generation: Approved Amount: {}, Interest Rate: {}, Duration: {}, Monthly EMI: {}, Loan Start Date: {}",
                loan.getApprovedAmount(), loan.getInterestRate(), loan.getDurationMonths(), loan.getMonthlyEMI(), loan.getLoanStartDate());

        if (loan.getApprovedAmount() == null || loan.getInterestRate() == null ||
                loan.getDurationMonths() == null || loan.getMonthlyEMI() == null ||
                loan.getLoanStartDate() == null) {
            log.error("Loan ID {} is missing required fields for repayment schedule generation. Approved Amount: {}, Interest Rate: {}, Duration: {}, Monthly EMI: {}, Loan Start Date: {}",
                    loan.getId(), loan.getApprovedAmount(), loan.getInterestRate(), loan.getDurationMonths(), loan.getMonthlyEMI(), loan.getLoanStartDate());
            throw new IllegalArgumentException("Loan must have approvedAmount, interestRate, durationMonths, monthlyEMI, and loanStartDate set for schedule generation.");
        }

        List<Repayment> repayments = new ArrayList<>();
        BigDecimal principal = loan.getApprovedAmount(); // Use approved amount as principal
        BigDecimal annualInterestRate = loan.getInterestRate();
        Integer durationMonths = loan.getDurationMonths();
        BigDecimal monthlyEMI = loan.getMonthlyEMI();

        BigDecimal remainingPrincipal = principal;
        // Start date for the first repayment (e.g., one month after loanStartDate)
        // Set to the first day of the next month after loanStartDate for consistency
        LocalDate repaymentDate = loan.getLoanStartDate().toLocalDate().plusMonths(1);
        repaymentDate = repaymentDate.withDayOfMonth(1); // Ensure it's the 1st of the month
        log.debug("Initial repayment date: {}", repaymentDate);


        BigDecimal monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        log.debug("Monthly rate for schedule: {}", monthlyRate);


        for (int i = 1; i <= durationMonths; i++) {
            BigDecimal interestAmount = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalAmount = monthlyEMI.subtract(interestAmount);

            log.debug("Installment {}: Remaining Principal before: {}, Interest: {}, Principal Payment: {}",
                    i, remainingPrincipal, interestAmount, principalAmount);

            // Special handling for the last installment to account for minor rounding errors
            if (i == durationMonths) {
                principalAmount = remainingPrincipal.setScale(2, RoundingMode.HALF_UP);
                // Adjust final EMI to precisely pay off the loan
                monthlyEMI = principalAmount.add(interestAmount).setScale(2, RoundingMode.HALF_UP);
                log.debug("Last installment adjustment. New principal amount: {}, New EMI: {}", principalAmount, monthlyEMI);
            } else if (principalAmount.compareTo(BigDecimal.ZERO) < 0) {
                // If principal becomes negative due to rounding, force it to zero
                principalAmount = BigDecimal.ZERO;
                log.warn("Principal amount became negative for installment {}. Forcing to zero.", i);
            }


            Repayment repayment = Repayment.builder()
                    .loan(loan)
                    .installmentNumber(i) // Added installment number
                    .dueDate(repaymentDate)
                    .principalAmount(principalAmount)
                    .interestAmount(interestAmount)
                    .totalPayment(monthlyEMI)
                    .status(Repayment.RepaymentStatus.PENDING)
                    .build();
            repayments.add(repayment);

            remainingPrincipal = remainingPrincipal.subtract(principalAmount);
            repaymentDate = repaymentDate.plusMonths(1);
            log.debug("Installment {} created. Due Date: {}, Remaining Principal after: {}", i, repaymentDate.minusMonths(1), remainingPrincipal);
        }

        try {
            repaymentRepository.saveAll(repayments);
            log.info("Generated and saved {} repayment records for Loan ID: {}", repayments.size(), loan.getId());
        } catch (Exception e) {
            log.error("Error saving repayment schedule for Loan ID {}: {}", loan.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save repayment schedule.", e);
        }
    }

    /**
     * Admin approves a loan application. This method sets the approved terms
     * and triggers EMI calculation and repayment schedule generation.
     */
    @Transactional
    public Loan approveLoan(Long loanId, BigDecimal approvedAmount, BigDecimal interestRate, Integer approvedDurationMonths, User adminUser) {
        log.info("Admin {} attempting to approve loan with ID: {}. Approved amount: {}, Interest rate: {}, Duration: {}",
                adminUser.getUsername(), loanId, approvedAmount, interestRate, approvedDurationMonths);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + loanId));
        log.debug("Found loan: {} with current status: {}", loan.getId(), loan.getStatus());

        if (loan.getStatus() != Loan.LoanStatus.PENDING) {
            log.error("Loan ID {} is not in PENDING status. Current status: {}", loanId, loan.getStatus());
            throw new IllegalArgumentException("Loan is not in PENDING status and cannot be approved. Current status: " + loan.getStatus());
        }

        // Set approved terms
        loan.setApprovedAmount(approvedAmount);
        loan.setInterestRate(interestRate);
        loan.setDurationMonths(approvedDurationMonths); // Use the approved duration from admin form
        loan.setApprovedDate(LocalDateTime.now());
        loan.setLoanStartDate(LocalDateTime.now()); // Loan starts now
        log.debug("Loan ID {} terms set: Approved Amount: {}, Interest Rate: {}, Duration: {}, Loan Start Date: {}",
                loanId, approvedAmount, interestRate, approvedDurationMonths, loan.getLoanStartDate());


        // Calculate EMI based on approved terms
        BigDecimal monthlyEMI = calculateEMI(approvedAmount, interestRate, approvedDurationMonths); // Pass approvedDurationMonths
        loan.setMonthlyEMI(monthlyEMI);
        log.debug("Loan ID {} calculated Monthly EMI: {}", loanId, monthlyEMI);

        // Update status to APPROVED (or DISBURSED if that's the next step after approval)
        // Assuming DISBURSED means approved and ready for repayments
        loan.setStatus(Loan.LoanStatus.DISBURSED); // Changed to DISBURSED as per common flow

        Loan approvedAndDisbursedLoan;
        try {
            approvedAndDisbursedLoan = loanRepository.save(loan);
            log.info("Loan with ID {} approved and disbursed by admin {}. Monthly EMI: {}", loanId, adminUser.getUsername(), monthlyEMI);
        } catch (Exception e) {
            log.error("Error saving approved loan with ID {}: {}", loanId, e.getMessage(), e);
            throw new RuntimeException("Failed to save approved loan.", e);
        }


        // Generate repayment schedule for the approved loan
        try {
            generateRepaymentSchedule(approvedAndDisbursedLoan);
            log.info("Repayment schedule generated successfully for Loan ID: {}", loanId);
        } catch (Exception e) {
            log.error("Error generating repayment schedule for Loan ID {}: {}", loanId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate repayment schedule.", e);
        }


        return approvedAndDisbursedLoan;
    }

    @Transactional
    public Loan rejectLoan(Long loanId, String reason, User adminUser) {
        log.info("Admin {} attempting to reject loan with ID: {} for reason: {}", adminUser.getUsername(), loanId, reason);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + loanId));

        if (loan.getStatus() != Loan.LoanStatus.PENDING) {
            throw new IllegalArgumentException("Loan is not in PENDING status and cannot be rejected. Current status: " + loan.getStatus());
        }

        loan.setStatus(Loan.LoanStatus.REJECTED);
        loan.setRejectedReason(reason);
        loan.setLastUpdated(LocalDateTime.now()); // Update last updated date on rejection
        Loan rejectedLoan = loanRepository.save(loan);
        log.info("Loan with ID {} rejected by admin {} for reason: {}", loanId, adminUser.getUsername(), reason);
        return rejectedLoan;
    }

    @Transactional(readOnly = true)
    public List<Loan> getAllLoans() {
        log.info("Fetching all loan applications.");
        return loanRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Loan> getPendingLoans() {
        log.info("Fetching all PENDING loan applications for admin review.");
        return loanRepository.findByStatus(Loan.LoanStatus.PENDING);
    }

    /**
     * Fetches loans based on their status.
     * This method is added to support filtering in the admin dashboard.
     * @param status The status to filter loans by.
     * @return A list of loans matching the given status.
     */
    @Transactional(readOnly = true)
    public List<Loan> getLoansByStatus(Loan.LoanStatus status) {
        log.info("Fetching loans by status: {}", status);
        return loanRepository.findByStatus(status);
    }
}
