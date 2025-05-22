package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.LoanApplicationRequest;
import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;

    @Transactional
    public Loan applyForLoan(LoanApplicationRequest request, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Authenticated user is required to apply for a loan.");
        }



        Loan newLoan = Loan.builder()
                .user(currentUser)
                .amount(request.getAmount())
                .interestRate(request.getInterestRate())
                .termMonths(request.getTermMonths())
                .purpose(request.getPurpose())
                .status(Loan.LoanStatus.PENDING)
                .build();

        return loanRepository.save(newLoan);
    }

    @Transactional(readOnly = true)
    public java.util.List<Loan> getLoansByUser(User user) {
        return loanRepository.findByUser(user);
    }


}