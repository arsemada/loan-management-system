package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.LoanApplicationRequest;
import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.entity.Repayment; // Assuming this is your Repayment schedule entity
import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.repository.LoanRepository;
import com.example.loanmanagement.repository.RepaymentRepository; // Assuming this exists
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
    private final RepaymentRepository repaymentRepository; // Used for saving repayment schedules

    /**
     * Allows a customer to apply for a loan.
     * Initial status is PENDING. EMI and schedule are NOT generated here.
     * They are generated upon admin approval.
     */
    @Transactional
    public Loan applyForLoan(LoanApplicationRequest request, User customer) { // Changed 'user' to 'customer' for clarity
        log.info("Attempting to apply for loan for user: {} with loanAmount: {}, durationMonths: {}, purpose: {}, annualIncome: {}",
                customer.getUsername(), request.getLoanAmount(), request.getDurationMonths(), request.getPurpose(), request.getAnnualIncome());

        // Build the loan object with details from the application request
        Loan newLoan = Loan.builder()
                .customer(customer) // Changed 'user' to 'customer'
                .loanAmount(request.getLoanAmount()) // Updated field name
                .loanType(request.getLoanType()) // NEW: Added loanType
                .durationMonths(request.getDurationMonths()) // Updated field name
                .purpose(request.getPurpose())
                .annualIncome(request.getAnnualIncome()) // NEW: Added annualIncome
                .status(Loan.LoanStatus.PENDING) // Initial status
                // interestRate, monthlyEMI, approvedAmount, loanStartDate are null initially
                // They will be set upon approval by an admin
                .build();

        Loan savedLoan = loanRepository.save(newLoan);
        log.info("Loan application saved successfully with ID: {}", savedLoan.getId());

        // DO NOT generate EMI or schedule here. That happens upon admin approval.

        return savedLoan;
    }

    @Transactional(readOnly = true)
    public List<Loan> getLoansByCustomer(User customer) { // Changed 'user' to 'customer'
        log.info("Fetching loans for customer: {}", customer.getUsername());
        return loanRepository.findByCustomer(customer); // Assumes you have findByCustomer in your LoanRepository
    }

    @Transactional(readOnly = true)
    public Optional<Loan> getLoanById(Long loanId) {
        log.info("Fetching loan by ID: {}", loanId);
        return loanRepository.findById(loanId);
    }

    @Transactional(readOnly = true)
    public List<Repayment> getRepaymentsForLoan(Loan loan) {
        log.info("Fetching repayments for loan ID: {}", loan.getId());
        // This assumes Repayment entity has a 'loan' field to link to the Loan
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
    public BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualInterestRate, Integer durationMonths) { // Renamed termMonths to durationMonths
        // Monthly interest rate in decimal form (e.g., 8% annual becomes 0.08/12 monthly)
        BigDecimal monthlyInterestRate = annualInterestRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        // Handle zero interest rate case (simple division)
        if (monthlyInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.HALF_UP);
        }

        // (1 + R)^N
        BigDecimal powFactor = BigDecimal.ONE.add(monthlyInterestRate).pow(durationMonths);

        // Numerator: P * R * (1 + R)^N
        BigDecimal numerator = principal.multiply(monthlyInterestRate).multiply(powFactor);

        // Denominator: (1 + R)^N - 1
        BigDecimal denominator = powFactor.subtract(BigDecimal.ONE);

        // Handle zero denominator (shouldn't happen with positive interest rate and term)
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Denominator for EMI calculation is zero. Check principal: {}, annualInterestRate: {}, durationMonths: {}",
                    principal, annualInterestRate, durationMonths);
            return principal.divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.HALF_UP); // Fallback to simple division
        }

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP); // Scale to 2 decimal places for currency
    }

    /**
     * Generates and saves the repayment schedule for an approved loan.
     */
    @Transactional
    public void generateRepaymentSchedule(Loan loan) {
        log.info("Starting repayment schedule generation for Loan ID: {}", loan.getId());

        // Ensure loan has necessary fields for schedule generation
        if (loan.getApprovedAmount() == null || loan.getInterestRate() == null ||
                loan.getDurationMonths() == null || loan.getMonthlyEMI() == null ||
                loan.getLoanStartDate() == null) {
            log.error("Loan ID {} is missing required fields for repayment schedule generation.", loan.getId());
            throw new IllegalArgumentException("Loan must have approvedAmount, interestRate, durationMonths, monthlyEMI, and loanStartDate set for schedule generation.");
        }

        List<Repayment> repayments = new ArrayList<>();
        BigDecimal principal = loan.getApprovedAmount(); // Use approved amount as principal
        BigDecimal annualInterestRate = loan.getInterestRate();
        Integer durationMonths = loan.getDurationMonths(); // Updated field name
        BigDecimal monthlyEMI = loan.getMonthlyEMI(); // Updated field name

        BigDecimal remainingPrincipal = principal;
        // Start date for the first repayment (e.g., one month after loanStartDate)
        LocalDate repaymentDate = loan.getLoanStartDate().toLocalDate().plusMonths(1);

        for (int i = 1; i <= durationMonths; i++) {
            BigDecimal monthlyInterestRate = annualInterestRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
            BigDecimal interestAmount = remainingPrincipal.multiply(monthlyInterestRate).setScale(2, RoundingMode.HALF_UP);

            // Calculate principal portion
            BigDecimal principalAmount = monthlyEMI.subtract(interestAmount);
            if (principalAmount.compareTo(BigDecimal.ZERO) < 0) {
                // This can happen if EMI is very small compared to interest (e.g., tiny loan balance)
                // or due to rounding. Adjust to ensure principal doesn't go negative.
                principalAmount = BigDecimal.ZERO;
            }

            // For the last payment, adjust principal to clear the remaining balance
            if (i == durationMonths) {
                principalAmount = remainingPrincipal.setScale(2, RoundingMode.HALF_UP);
                monthlyEMI = principalAmount.add(interestAmount).setScale(2, RoundingMode.HALF_UP); // Adjust final EMI
            }

            Repayment repayment = Repayment.builder()
                    .loan(loan)
                    .dueDate(repaymentDate)
                    .principalAmount(principalAmount.setScale(2, RoundingMode.HALF_UP))
                    .interestAmount(interestAmount.setScale(2, RoundingMode.HALF_UP))
                    .totalPayment(monthlyEMI.setScale(2, RoundingMode.HALF_UP)) // Total payment for this installment
                    .status(Repayment.RepaymentStatus.PENDING)
                    .build();
            repayments.add(repayment);

            remainingPrincipal = remainingPrincipal.subtract(principalAmount);
            repaymentDate = repaymentDate.plusMonths(1);
        }

        repaymentRepository.saveAll(repayments);
        log.info("Generated {} repayment records for Loan ID: {}", repayments.size(), loan.getId());
    }

    /**
     * Admin approves a loan application. This method sets the approved terms
     * and triggers EMI calculation and repayment schedule generation.
     */
    @Transactional
    public Loan approveLoan(Long loanId, BigDecimal approvedAmount, BigDecimal interestRate, Integer loanDurationMonths, User adminUser) {
        log.info("Admin {} attempting to approve loan with ID: {}. Approved amount: {}, Interest rate: {}, Duration: {}",
                adminUser.getUsername(), loanId, approvedAmount, interestRate, loanDurationMonths);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + loanId));

        if (loan.getStatus() != Loan.LoanStatus.PENDING) {
            throw new IllegalArgumentException("Loan is not in PENDING status and cannot be approved. Current status: " + loan.getStatus());
        }

        // Set approved terms
        loan.setApprovedAmount(approvedAmount);
        loan.setInterestRate(interestRate);
        loan.setDurationMonths(loanDurationMonths); // Ensure this matches the approved duration
        loan.setApprovedDate(LocalDateTime.now());
        loan.setLoanStartDate(LocalDateTime.now()); // Loan starts now, first EMI due next month

        // Calculate EMI based on approved terms
        BigDecimal monthlyEMI = calculateEMI(approvedAmount, interestRate, loanDurationMonths);
        loan.setMonthlyEMI(monthlyEMI);

        // Update status to DISBURSED (as per mentor's brief, loan start date implies disbursement)
        loan.setStatus(Loan.LoanStatus.DISBURSED);

        Loan approvedAndDisbursedLoan = loanRepository.save(loan);
        log.info("Loan with ID {} approved and disbursed by admin {}. Monthly EMI: {}", loanId, adminUser.getUsername(), monthlyEMI);

        // Generate repayment schedule for the approved loan
        generateRepaymentSchedule(approvedAndDisbursedLoan);

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
        Loan rejectedLoan = loanRepository.save(loan);
        log.info("Loan with ID {} rejected by admin {} for reason: {}", loanId, adminUser.getUsername(), reason);
        return rejectedLoan;
    }

    @Transactional(readOnly = true)
    public List<Loan> getAllPendingLoans() {
        log.info("Fetching all PENDING loan applications for admin review.");
        return loanRepository.findByStatus(Loan.LoanStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<Loan> getLoansByStatus(Loan.LoanStatus status) {
        log.info("Fetching loans with status: {}", status);
        return loanRepository.findByStatus(status);
    }

    // You will need to add methods for updating repayment status and other admin/customer actions later.
    // E.g., recordPayment(), getOverdueLoans(), etc.
}