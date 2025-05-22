package com.example.loanmanagement.repository;

import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {


    List<Loan> findByUser(User user);


}