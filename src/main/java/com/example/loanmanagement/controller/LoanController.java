// Example: In a LoanController.java file

package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.LoanApplicationRequest;
import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    public ResponseEntity<?> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        if (currentUser == null) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
        }
        try {

            loanService.applyForLoan(request, currentUser);

            return ResponseEntity.status(HttpStatus.CREATED).body("Loan application submitted successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to submit loan application: " + e.getMessage());
        }
    }


}