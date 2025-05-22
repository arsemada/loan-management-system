package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.RegistrationRequest;
import com.example.loanmanagement.entity.Role;
import com.example.loanmanagement.entity.User; // Import the User entity
import com.example.loanmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

            e.printStackTrace(); // This is crucial for debugging
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
            model.addAttribute("user", currentUser); // Add the full user object to the model
            model.addAttribute("firstName", currentUser.getFirstName());
            model.addAttribute("lastName", currentUser.getLastName());
            model.addAttribute("email", currentUser.getEmail());
        } else {
            return "redirect:/login";
        }

        return "dashboard";
    }
}