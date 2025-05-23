package com.example.loanmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequest {

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "100.00", message = "Loan amount must be at least $100.00")
    @DecimalMax(value = "10000000.00", message = "Loan amount cannot exceed $10,000,000.00")
    private BigDecimal amount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.10", message = "Interest rate must be at least 0.10%")
    @DecimalMax(value = "30.00", message = "Interest rate cannot exceed 30.00%")
    private BigDecimal interestRate;

    @NotNull(message = "Loan term is required")
    @Min(value = 6, message = "Loan term must be at least 6 months")
    @Max(value = 360, message = "Loan term cannot exceed 360 months (30 years)")
    private Integer termMonths;

    private String purpose;
}