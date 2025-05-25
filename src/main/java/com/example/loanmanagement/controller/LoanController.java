// Example: In a LoanController.java file

package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.LoanApplicationRequest;
import com.example.loanmanagement.entity.User; // Assuming you'll inject the authenticated user
import com.example.loanmanagement.service.LoanService; // Assuming you have this service
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // To get the authenticated user
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid; // Don't forget this import for DTO validation

@RestController
@RequestMapping("/api/loans") // <--- THIS IS THE BASE PATH
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService; // Assuming you have a LoanService

    @PostMapping("/apply") // <--- THIS IS THE SPECIFIC ENDPOINT
    public ResponseEntity<?> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request,
            @AuthenticationPrincipal User currentUser // Assuming your User entity implements UserDetails
    ) {
        if (currentUser == null) {
            // This case should ideally be caught by Spring Security's authentication filter,
            // but it's a good defensive check if the filter allows anonymous access to some degree
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
        }
        try {
            // You would call your LoanService here to process the application
            // Example:
            loanService.applyForLoan(request, currentUser); // Implement this method in LoanService

            return ResponseEntity.status(HttpStatus.CREATED).body("Loan application submitted successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // Log the actual exception for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to submit loan application: " + e.getMessage());
        }
    }

    // ... other loan-related API endpoints like /my, /{id}/repayment etc.
}