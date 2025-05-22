package com.example.loanmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequest {

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "0.01", message = "Loan amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.00", message = "Interest rate cannot be negative")
    private BigDecimal interestRate;

    @NotNull(message = "Loan term is required")
    @Min(value = 1, message = "Loan term must be at least 1 month")
    private Integer termMonths;

    private String purpose;
}