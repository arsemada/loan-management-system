package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.RegistrationRequest;
import com.example.loanmanagement.entity.Role;
import com.example.loanmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class WebPageController {

    private final UserService userService;


    @GetMapping("/register")
    public String showRegistrationForm(Model model) {

        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "register";
    }


    @PostMapping("/register")
    public String processRegistration(@ModelAttribute RegistrationRequest request, RedirectAttributes redirectAttributes) {
        try {

            request.setRole(Role.CUSTOMER);
            userService.registerNewUser(request);


            redirectAttributes.addFlashAttribute("success", "Registration successful! Please log in.");

            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

            return "redirect:/register";
        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during registration.");
            return "redirect:/register";
        }
    }


    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "registered", required = false) String registered,
                                Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("logout", "You have been logged out.");
        }
        if (registered != null) {
            model.addAttribute("registered", "Registration successful! Please log in.");
        }
        return "login";
    }

    @GetMapping("/dashboard")
    public String showDashboard() {
        return "dashboard";
    }
}