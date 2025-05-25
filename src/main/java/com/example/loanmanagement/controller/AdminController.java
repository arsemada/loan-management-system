package com.example.loanmanagement.controller;

import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal; // Import for BigDecimal
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final LoanService loanService;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model,
                                 @RequestParam(value = "filter", required = false, defaultValue = "PENDING") String filter) {
        List<Loan> loans;
        try {
            Loan.LoanStatus statusFilter = Loan.LoanStatus.valueOf(filter.toUpperCase());
            loans = loanService.getLoansByStatus(statusFilter);
            model.addAttribute("filter", statusFilter.name());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid loan status filter received: {}", filter);
            // Fallback to PENDING if the filter is invalid
            loans = loanService.getAllPendingLoans();
            model.addAttribute("filter", "PENDING");
        }

        model.addAttribute("loans", loans);
        return "admin-dashboard"; // Ensure you have an 'admin-dashboard.html' Thymeleaf template
    }

    @PostMapping("/loan/approve/{id}")
    public String approveLoan(@PathVariable("id") Long loanId,
                              @RequestParam("approvedAmount") BigDecimal approvedAmount, // NEW: Get approved amount from form
                              @RequestParam("interestRate") BigDecimal interestRate,     // NEW: Get interest rate from form
                              @RequestParam("loanDurationMonths") Integer loanDurationMonths, // NEW: Get duration from form
                              @AuthenticationPrincipal User adminUser,
                              RedirectAttributes redirectAttributes) {
        try {
            // Validate input for approved amount, interest rate, and duration (basic check)
            if (approvedAmount == null || approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("error", "Approved amount must be positive.");
                return "redirect:/admin/dashboard";
            }
            if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) < 0) {
                redirectAttributes.addFlashAttribute("error", "Interest rate must be non-negative.");
                return "redirect:/admin/dashboard";
            }
            if (loanDurationMonths == null || loanDurationMonths <= 0) {
                redirectAttributes.addFlashAttribute("error", "Loan duration must be positive.");
                return "redirect:/admin/dashboard";
            }

            // Call the updated service method with all required parameters
            loanService.approveLoan(loanId, approvedAmount, interestRate, loanDurationMonths, adminUser);
            redirectAttributes.addFlashAttribute("success", "Loan ID " + loanId + " approved successfully and disbursed.");
        } catch (IllegalArgumentException e) {
            log.error("Validation/Business logic error approving loan ID {}: {}", loanId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("An unexpected error occurred approving loan ID {}: {}", loanId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during loan approval. Please try again.");
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/loan/reject/{id}")
    public String rejectLoan(@PathVariable("id") Long loanId,
                             @RequestParam("reason") String reason,
                             @AuthenticationPrincipal User adminUser,
                             RedirectAttributes redirectAttributes) {
        try {
            if (reason == null || reason.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Rejection reason cannot be empty.");
                return "redirect:/admin/dashboard";
            }
            loanService.rejectLoan(loanId, reason, adminUser);
            redirectAttributes.addFlashAttribute("success", "Loan ID " + loanId + " rejected successfully.");
        } catch (IllegalArgumentException e) {
            log.error("Validation/Business logic error rejecting loan ID {}: {}", loanId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("An unexpected error occurred rejecting loan ID {}: {}", loanId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during loan rejection.");
        }
        return "redirect:/admin/dashboard";
    }
}