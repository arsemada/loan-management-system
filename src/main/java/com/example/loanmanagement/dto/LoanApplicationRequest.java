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
    private BigDecimal loanAmount;

    @NotNull(message = "Loan type is required")
    private LoanType loanType;

    @NotNull(message = "Loan duration is required")
    @Min(value = 6, message = "Loan duration must be at least 6 months")
    @Max(value = 360, message = "Loan duration cannot exceed 360 months (30 years)")
    private Integer durationMonths;

    @NotBlank(message = "Purpose is required")
    @Size(max = 255, message = "Purpose cannot exceed 255 characters")
    private String purpose;

    @NotNull(message = "Annual income is required")
    @DecimalMin(value = "0.00", message = "Annual income cannot be negative")
    private BigDecimal annualIncome;


}