package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.LoanApplicationRequest;
import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;

    @Transactional
    public Loan applyForLoan(LoanApplicationRequest request, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Authenticated user is required to apply for a loan.");
        }

        BigDecimal monthlyPayment = calculateEMI(request.getAmount(), request.getInterestRate(), request.getTermMonths());

        Loan newLoan = Loan.builder()
                .user(currentUser)
                .amount(request.getAmount())
                .interestRate(request.getInterestRate())
                .termMonths(request.getTermMonths())
                .monthlyPayment(monthlyPayment)
                .purpose(request.getPurpose())
                .status(Loan.LoanStatus.PENDING)
                .build();

        return loanRepository.save(newLoan);
    }

    @Transactional(readOnly = true)
    public java.util.List<Loan> getLoansByUser(User user) {
        return loanRepository.findByUser(user);
    }

    public BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualInterestRate, Integer termMonths) {


        if (principal == null || annualInterestRate == null || termMonths == null ||
                principal.compareTo(BigDecimal.ZERO) <= 0 || termMonths <= 0) {
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
        BigDecimal powOnePlusMonthlyRate;

        powOnePlusMonthlyRate = onePlusMonthlyRate.pow(termMonths).setScale(CALC_SCALE, RoundingMode.HALF_UP);

        BigDecimal numerator = principal
                .multiply(monthlyRate)
                .multiply(powOnePlusMonthlyRate);
        numerator = numerator.setScale(CALC_SCALE, RoundingMode.HALF_UP);

        BigDecimal denominator = powOnePlusMonthlyRate.subtract(BigDecimal.ONE);
        denominator = denominator.setScale(CALC_SCALE, RoundingMode.HALF_UP);


        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("EMI calculation failed: denominator is zero. This might happen with very small interest rates, or precision issues for short terms.");
        }

        return numerator.divide(denominator, RESULT_SCALE, RoundingMode.HALF_UP);
    }
}