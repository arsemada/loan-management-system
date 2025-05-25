package com.example.loanmanagement.config; // Adjust if your config package is different

import com.example.loanmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // Tells Spring this class has beans
@RequiredArgsConstructor // Automatically creates constructor for final fields
public class AdminCreator {

    private final UserService userService; // Spring will automatically give us the UserService

    // @Bean // <--- THIS LINE IS NOW COMMENTED OUT!
    public CommandLineRunner createDefaultAdmin() {
        return args -> {
            String adminEmail = "new_admin@example.com"; // <--- CHANGE THIS EMAIL IF NEEDED
            String adminUsername = "new_admin";           // <--- CHANGE THIS USERNAME IF NEEDED
            String adminPassword = "very_secret_admin_password"; // <--- VERY IMPORTANT: USE A STRONG PASSWORD HERE AND REMEMBER IT!

            try {
                // Try to find the admin by email. If found, do nothing.
                userService.loadUserByUsername(adminEmail);
                System.out.println("Admin user '" + adminEmail + "' already exists. Skipping creation.");
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                // If not found, create the new admin user
                System.out.println("Creating new admin user: " + adminEmail);
                // IMPORTANT: This calls a method in your UserService that must handle creating an ADMIN role.
                // I assume you have a method like createAdminUser(username, email, password, firstName, lastName)
                // in your UserService from previous discussions.
                userService.createAdminUser(adminUsername, adminEmail, adminPassword, "New", "Admin");
                System.out.println("New admin user '" + adminEmail + "' created successfully!");
            } catch (Exception e) {
                // Catch any other unexpected errors during creation
                System.err.println("Error creating admin user: " + e.getMessage());
            }
        };
    }
}