// src/main/java/com/example/loanmanagement/repository/LoanRepository.java
package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.entity.User; // Assuming your User entity is here
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByCustomer(User customer);
    List<Loan> findByStatus(Loan.LoanStatus status);

}