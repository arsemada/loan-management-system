package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.entity.User; // Import User if you plan to search by user
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Custom query to find all loans for a specific user
    List<Loan> findByUser(User user);

    // You might add more specific queries later, e.g.:
    // List<Loan> findByStatus(Loan.LoanStatus status);
    // List<Loan> findByUserAndStatus(User user, Loan.LoanStatus status);
}