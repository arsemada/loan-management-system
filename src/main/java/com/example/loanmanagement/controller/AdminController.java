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
            // CORRECTED LINE: Call the new service method
            loans = loanService.getLoansByStatus(statusFilter);
            model.addAttribute("filter", statusFilter.name());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid loan status filter received: {}", filter);
            // Fallback to PENDING if the filter is invalid
            loans = loanService.getAllPendingLoans();
            model.addAttribute("filter", "PENDING");
        }

        model.addAttribute("loans", loans);
        return "admin-dashboard";
    }

    @PostMapping("/loan/approve/{id}")
    public String approveLoan(@PathVariable("id") Long loanId,
                              @AuthenticationPrincipal User adminUser,
                              RedirectAttributes redirectAttributes) {
        try {
            loanService.approveLoan(loanId, adminUser);
            redirectAttributes.addFlashAttribute("success", "Loan ID " + loanId + " approved successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error approving loan ID {}: {}", loanId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during loan approval.");
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
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error rejecting loan ID {}: {}", loanId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during loan rejection.");
        }
        return "redirect:/admin/dashboard";
    }
}