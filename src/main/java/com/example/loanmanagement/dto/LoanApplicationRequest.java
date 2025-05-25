package com.example.loanmanagement.dto;

import com.example.loanmanagement.entity.LoanType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequest {

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "100.00", message = "Loan amount must be at least $100.00")
    @DecimalMax(value = "10000000.00", message = "Loan amount cannot exceed $10,000,000.00")
    private BigDecimal loanAmount; // Keep this as is

    @NotNull(message = "Loan type is required")
    private LoanType loanType; // Keep this as is

    @NotNull(message = "Loan duration is required")
    @Min(value = 6, message = "Loan duration must be at least 6 months")
    @Max(value = 360, message = "Loan duration cannot exceed 360 months (30 years)")
    private Integer durationMonths; // Keep this as is

    @NotBlank(message = "Purpose is required")
    @Size(max = 255, message = "Purpose cannot exceed 255 characters")
    private String purpose; // Keep this as is

    @NotNull(message = "Annual income is required")
    @DecimalMin(value = "0.00", message = "Annual income cannot be negative")
    private BigDecimal annualIncome; // Keep this as is

    // IMPORTANT: You have an interestRate field in your HTML but not in your DTO.
    // If you need to capture interest rate from the form, you MUST add it here:
    // private BigDecimal interestRate;
    // And add appropriate validation annotations.
    // For now, I will assume it's not meant to be submitted via the form,
    // or it's a value calculated on the backend.
    // If you need it, uncomment the line below and add validation.
    // private BigDecimal interestRate;
}