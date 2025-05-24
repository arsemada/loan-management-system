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

    @Transactional
    public Loan applyForLoan(LoanApplicationRequest request, User user) {
        log.info("Attempting to apply for loan for user: {} with amount: {}, term: {}, purpose: {}",
                user.getUsername(), request.getAmount(), request.getTermMonths(), request.getPurpose());


        BigDecimal monthlyPayment = calculateEMI(request.getAmount(), request.getInterestRate(), request.getTermMonths());

        Loan newLoan = Loan.builder()
                .user(user)
                .amount(request.getAmount())
                .interestRate(request.getInterestRate())
                .termMonths(request.getTermMonths())
                .monthlyPayment(monthlyPayment)
                .status(Loan.LoanStatus.PENDING)
                .purpose(request.getPurpose())
                .build();

        Loan savedLoan = loanRepository.save(newLoan);
        log.info("Loan saved successfully with ID: {}. Generating repayment schedule.", savedLoan.getId());

        try {
            generateRepaymentSchedule(savedLoan);
            log.info("Repayment schedule generated and saved for Loan ID: {}", savedLoan.getId());
        } catch (Exception e) {
            log.error("Failed to generate repayment schedule for Loan ID: {}. Error: {}", savedLoan.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate repayment schedule: " + e.getMessage(), e);
        }

        return savedLoan;
    }

    @Transactional(readOnly = true)
    public List<Loan> getLoansByUser(User user) {
        log.info("Fetching loans for user: {}", user.getUsername());
        return loanRepository.findByUser(user);
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

    private BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualInterestRate, Integer termMonths) {
        BigDecimal monthlyInterestRate = annualInterestRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP); // Divide by 100 for percentage and 12 for months

        if (monthlyInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }



        BigDecimal numerator = monthlyInterestRate.multiply(
                BigDecimal.ONE.add(monthlyInterestRate).pow(termMonths)
        );
        BigDecimal denominator = BigDecimal.ONE.add(monthlyInterestRate).pow(termMonths).subtract(BigDecimal.ONE);

        if (denominator.compareTo(BigDecimal.ZERO) == 0) {

            return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }

        return principal.multiply(numerator).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void generateRepaymentSchedule(Loan loan) {
        log.info("Starting repayment schedule generation for Loan ID: {}", loan.getId());
        List<Repayment> repayments = new ArrayList<>();
        BigDecimal principal = loan.getAmount();
        BigDecimal annualInterestRate = loan.getInterestRate();
        Integer termMonths = loan.getTermMonths();
        BigDecimal monthlyPayment = loan.getMonthlyPayment();

        BigDecimal remainingPrincipal = principal;
        LocalDate repaymentDate = LocalDate.now().plusMonths(1);

        for (int i = 1; i <= termMonths; i++) {
            BigDecimal monthlyInterestRate = annualInterestRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
            BigDecimal interestAmount = remainingPrincipal.multiply(monthlyInterestRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalAmount = monthlyPayment.subtract(interestAmount).setScale(2, RoundingMode.HALF_UP);

            if (i == termMonths) {
                principalAmount = remainingPrincipal.setScale(2, RoundingMode.HALF_UP);
                monthlyPayment = principalAmount.add(interestAmount).setScale(2, RoundingMode.HALF_UP);
            }

            Repayment repayment = Repayment.builder()
                    .loan(loan)
                    .dueDate(repaymentDate)
                    .principalAmount(principalAmount)
                    .interestAmount(interestAmount)
                    .totalPayment(monthlyPayment)
                    .status(Repayment.RepaymentStatus.PENDING)
                    .build();
            repayments.add(repayment);

            remainingPrincipal = remainingPrincipal.subtract(principalAmount);
            repaymentDate = repaymentDate.plusMonths(1);
        }

        repaymentRepository.saveAll(repayments);
        log.info("Generated {} repayment records for Loan ID: {}", repayments.size(), loan.getId());
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

    @Transactional
    public Loan approveLoan(Long loanId, User adminUser) {
        log.info("Admin {} attempting to approve loan with ID: {}", adminUser.getUsername(), loanId); // Changed to getUsername for consistency
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + loanId));

        if (loan.getStatus() != Loan.LoanStatus.PENDING) {
            throw new IllegalArgumentException("Loan is not in PENDING status and cannot be approved. Current status: " + loan.getStatus());
        }

        loan.setStatus(Loan.LoanStatus.APPROVED);
        loan.setApprovedDate(LocalDateTime.now());
        Loan approvedLoan = loanRepository.save(loan);
        log.info("Loan with ID {} approved by admin {}", loanId, adminUser.getUsername());
        return approvedLoan;
    }

    @Transactional
    public Loan rejectLoan(Long loanId, String reason, User adminUser) {
        log.info("Admin {} attempting to reject loan with ID: {} for reason: {}", adminUser.getUsername(), loanId, reason); // Changed to getUsername for consistency
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
}