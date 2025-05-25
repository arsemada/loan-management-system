package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.Repayment;
import com.example.loanmanagement.entity.Loan; // Import Loan entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Long> {
    // Find all repayments for a specific loan, ordered by due date
    List<Repayment> findByLoanOrderByDueDateAsc(Loan loan);
    // You might also need: List<Repayment> findByLoanIdOrderByDueDateAsc(Long loanId);
    // The Repayment entity has a 'loan' field (object), so findByLoan is appropriate.
}