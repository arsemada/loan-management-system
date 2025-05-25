package com.example.loanmanagement.controller;

import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/loans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Restrict access to ADMIN role only
public class AdminLoanController {

    private final LoanService loanService;

    // Displays a list of all loans (or ideally, just pending ones for review)
    @GetMapping
    public String listAllLoans(Model model) {
        // You might want to filter by PENDING status for the main admin view
        List<Loan> pendingLoans = loanService.getPendingLoans();
        model.addAttribute("loans", pendingLoans);
        model.addAttribute("allLoans", loanService.getAllLoans()); // Also provide all for a different tab/view if needed
        return "admin-loans"; // points to src/main/resources/templates/admin-loans.html
    }

    // Displays detailed view of a single loan for admin review/action
    @GetMapping("/{id}")
    public String viewLoanDetailsForAdmin(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Loan loan = loanService.getLoanById(id)
                .orElse(null);

        if (loan == null) {
            redirectAttributes.addFlashAttribute("error", "Loan not found with ID: " + id);
            return "redirect:/admin/loans";
        }

        model.addAttribute("loan", loan);
        // Fetch repayments only if the loan is in a state where a schedule exists
        if (loan.getStatus() == Loan.LoanStatus.APPROVED ||
                loan.getStatus() == Loan.LoanStatus.DISBURSED ||
                loan.getStatus() == Loan.LoanStatus.PAID ||
                loan.getStatus() == Loan.LoanStatus.OVERDUE) {
            model.addAttribute("repayments", loanService.getRepaymentsForLoan(loan));
        } else {
            model.addAttribute("repayments", new java.util.ArrayList<>()); // Send empty list if not applicable
        }
        return "admin-loan-details"; // points to src/main/resources/templates/admin-loan-details.html
    }

    // Handles loan approval
    @PostMapping("/{id}/approve")
    public String approveLoan(@PathVariable Long id,
                              @RequestParam("approvedAmount") BigDecimal approvedAmount,
                              @RequestParam("interestRate") BigDecimal interestRate,
                              @RequestParam("durationMonths") Integer durationMonths, // Get approved duration from form
                              @AuthenticationPrincipal User adminUser,
                              RedirectAttributes redirectAttributes) {
        try {
            Loan approvedLoan = loanService.approveLoan(id, approvedAmount, interestRate, durationMonths, adminUser);
            redirectAttributes.addFlashAttribute("success", "Loan ID " + approvedLoan.getId() + " approved and disbursed successfully!");
            return "redirect:/admin/loans/" + approvedLoan.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Error approving loan: " + e.getMessage());
            return "redirect:/admin/loans/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during loan approval.");
            return "redirect:/admin/loans/" + id;
        }
    }

    // Handles loan rejection
    @PostMapping("/{id}/reject")
    public String rejectLoan(@PathVariable Long id,
                             @RequestParam("rejectedReason") String rejectedReason,
                             @AuthenticationPrincipal User adminUser,
                             RedirectAttributes redirectAttributes) {
        try {
            Loan rejectedLoan = loanService.rejectLoan(id, rejectedReason, adminUser);
            redirectAttributes.addFlashAttribute("success", "Loan ID " + rejectedLoan.getId() + " rejected successfully!");
            return "redirect:/admin/loans/" + rejectedLoan.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Error rejecting loan: " + e.getMessage());
            return "redirect:/admin/loans/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during loan rejection.");
            return "redirect:/admin/loans/" + id;
        }
    }
}