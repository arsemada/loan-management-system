package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.LoanApplicationRequest;
import com.example.loanmanagement.dto.RegistrationRequest;
import com.example.loanmanagement.entity.Role;
import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.entity.Loan;
import com.example.loanmanagement.service.LoanService;
import com.example.loanmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebPageController {

    private final UserService userService;
    private final LoanService loanService;

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute RegistrationRequest request, RedirectAttributes redirectAttributes) {
        try {
            request.setRole(Role.CUSTOMER);
            userService.registerNewUser(request);

            redirectAttributes.addFlashAttribute("registered", "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during registration.");
            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "registered", required = false) String registered,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("logout", "You have been logged out successfully.");
        }
        if (registered != null) {
            model.addAttribute("registered", "Registration successful! Please log in.");
        }
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User currentUser, Model model) {
        if (currentUser != null) {
            model.addAttribute("user", currentUser);
            model.addAttribute("firstName", currentUser.getFirstName());
            model.addAttribute("lastName", currentUser.getLastName());
            model.addAttribute("email", currentUser.getEmail());
        } else {
            return "redirect:/login";
        }
        return "dashboard";
    }

    @GetMapping("/apply-loan")
    public String showApplyLoanForm(Model model) {
        model.addAttribute("loanApplicationRequest", new LoanApplicationRequest());
        return "apply-loan";
    }

    @PostMapping("/apply-loan")
    public String processLoanApplication(@Valid @ModelAttribute LoanApplicationRequest request,
                                         BindingResult bindingResult,
                                         @AuthenticationPrincipal User currentUser,
                                         RedirectAttributes redirectAttributes) {
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to apply for a loan.");
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please correct the errors in your application.");
            return "redirect:/apply-loan";
        }

        try {
            loanService.applyForLoan(request, currentUser);
            redirectAttributes.addFlashAttribute("success", "Loan application submitted successfully! It is now PENDING review.");
            // Now that /my-loans exists, we can redirect there directly
            return "redirect:/my-loans";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/apply-loan";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during loan application.");
            return "redirect:/apply-loan";
        }
    }


    @GetMapping("/my-loans")
    public String myLoans(@AuthenticationPrincipal User currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Fetch loans for the current user
        List<Loan> userLoans = loanService.getLoansByUser(currentUser);
        model.addAttribute("loans", userLoans);

        return "my-loans";
    }
}