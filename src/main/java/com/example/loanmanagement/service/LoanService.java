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
    public Loan applyForLoan(LoanApplicationRequest request, User currentUser) {
        log.info("Attempting to apply for loan for user: {}", currentUser.getEmail());
        if (currentUser == null) {
            throw new IllegalArgumentException("Authenticated user is required to apply for a loan.");
        }

        BigDecimal monthlyPayment = calculateEMI(request.getAmount(), request.getInterestRate(), request.getTermMonths());
        log.info("Calculated EMI: {}", monthlyPayment);

        Loan newLoan = Loan.builder()
                .user(currentUser)
                .amount(request.getAmount())
                .interestRate(request.getInterestRate())
                .termMonths(request.getTermMonths())
                .monthlyPayment(monthlyPayment)
                .purpose(request.getPurpose())
                .status(Loan.LoanStatus.PENDING)
                .build();

        Loan savedLoan = loanRepository.save(newLoan);
        log.info("Loan saved successfully with ID: {}", savedLoan.getId());

        try {
            generateRepaymentSchedule(savedLoan);
            log.info("Repayment schedule generated and saved for Loan ID: {}", savedLoan.getId());
        } catch (Exception e) {
            log.error("Error generating or saving repayment schedule for Loan ID {}: {}", savedLoan.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate repayment schedule", e);
        }


        return savedLoan;
    }

    @Transactional(readOnly = true)
    public List<Loan> getLoansByUser(User user) {
        log.debug("Fetching loans for user: {}", user.getEmail());
        return loanRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public Optional<Loan> getLoanById(Long loanId) {
        log.debug("Fetching loan by ID: {}", loanId);
        return loanRepository.findById(loanId);
    }

    @Transactional(readOnly = true)
    public List<Repayment> getRepaymentsForLoan(Loan loan) {
        log.debug("Fetching repayments for Loan ID: {}", loan.getId());
        List<Repayment> repayments = repaymentRepository.findByLoanOrderByDueDateAsc(loan);
        log.debug("Found {} repayments for Loan ID: {}", repayments.size(), loan.getId());
        return repayments;
    }


    public BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualInterestRate, Integer termMonths) {
        // ... (unchanged) ...
        if (principal == null || annualInterestRate == null || termMonths == null ||
                principal.compareTo(BigDecimal.ZERO) <= 0 || termMonths <= 0) {
            log.error("Invalid input for EMI calculation: Principal={}, Rate={}, Term={}", principal, annualInterestRate, termMonths);
            throw new IllegalArgumentException("Principal, annual interest rate, and term months must be positive values.");
        }

        final int CALC_SCALE = 20;
        final int RESULT_SCALE = 2;

        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(termMonths), RESULT_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualInterestRate
                .divide(new BigDecimal(100), CALC_SCALE, RoundingMode.HALF_UP)
                .divide(new BigDecimal(12), CALC_SCALE, RoundingMode.HALF_UP);

        BigDecimal onePlusMonthlyRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powOnePlusMonthlyRate = onePlusMonthlyRate.pow(termMonths).setScale(CALC_SCALE, RoundingMode.HALF_UP);

        BigDecimal numerator = principal
                .multiply(monthlyRate)
                .multiply(powOnePlusMonthlyRate);
        numerator = numerator.setScale(CALC_SCALE, RoundingMode.HALF_UP);

        BigDecimal denominator = powOnePlusMonthlyRate.subtract(BigDecimal.ONE);
        denominator = denominator.setScale(CALC_SCALE, RoundingMode.HALF_UP);


        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            log.error("EMI calculation failed: denominator is zero for Principal={}, Rate={}, Term={}", principal, annualInterestRate, termMonths);
            throw new ArithmeticException("EMI calculation failed: denominator is zero. This might happen with very small interest rates, or precision issues for short terms.");
        }

        return numerator.divide(denominator, RESULT_SCALE, RoundingMode.HALF_UP);
    }

    @Transactional
    public void generateRepaymentSchedule(Loan loan) {
        log.info("Starting repayment schedule generation for Loan ID: {}", loan.getId());
        BigDecimal outstandingPrincipal = loan.getAmount();
        BigDecimal monthlyRate = loan.getInterestRate()
                .divide(new BigDecimal(100), 10, RoundingMode.HALF_UP)
                .divide(new BigDecimal(12), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = loan.getMonthlyPayment();
        Integer termMonths = loan.getTermMonths();
        LocalDate nextDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);

        List<Repayment> repayments = new ArrayList<>();

        for (int i = 0; i < termMonths; i++) {
            BigDecimal interestForMonth = outstandingPrincipal.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP); // Rounded interest
            BigDecimal principalPaidThisMonth;

            if (i == termMonths - 1) {
                principalPaidThisMonth = outstandingPrincipal.subtract(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP); // Remaining principal
                monthlyPayment = principalPaidThisMonth.add(interestForMonth).setScale(2, RoundingMode.HALF_UP); // Adjust last EMI
            } else {
                principalPaidThisMonth = monthlyPayment.subtract(interestForMonth);
            }

            if (principalPaidThisMonth.compareTo(BigDecimal.ZERO) < 0) {
                principalPaidThisMonth = BigDecimal.ZERO;
            }


            log.debug("  Installment {}: Due Date={}, Principal={}, Interest={}, TotalPayment={}, OutstandingPrincipal={}",
                    i + 1, nextDueDate, monthlyPayment, principalPaidThisMonth, interestForMonth, outstandingPrincipal.subtract(principalPaidThisMonth));

            Repayment repayment = Repayment.builder()
                    .loan(loan)
                    .dueDate(nextDueDate)
                    .principalAmount(principalPaidThisMonth)
                    .interestAmount(interestForMonth)
                    .totalPayment(monthlyPayment)
                    .status(Repayment.RepaymentStatus.PENDING)
                    .build();

            repayments.add(repayment);
            outstandingPrincipal = outstandingPrincipal.subtract(principalPaidThisMonth);
            nextDueDate = nextDueDate.plusMonths(1);
        }

        log.info("Generated {} repayment records for Loan ID: {}", repayments.size(), loan.getId());
        repaymentRepository.saveAll(repayments);
        log.info("Successfully saved all repayments for Loan ID: {}", loan.getId());
    }
}