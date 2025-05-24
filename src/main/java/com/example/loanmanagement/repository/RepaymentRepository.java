package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.Repayment;
import com.example.loanmanagement.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Long> {
    List<Repayment> findByLoanOrderByDueDateAsc(Loan loan);
}