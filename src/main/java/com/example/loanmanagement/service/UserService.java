package com.example.loanmanagement.service;

import com.example.loanmanagement.entity.Role;
import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.repository.UserRepository;
import com.example.loanmanagement.dto.RegistrationRequest;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <-- ENSURE THIS IMPORT IS PRESENT

import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // <--- This is GREAT!

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // First try to find by username (which we'll set to email on registration)
        Optional<User> userOptional = userRepository.findByUsername(usernameOrEmail);

        if (userOptional.isEmpty()) {
            // If not found by username, try to find by email
            userOptional = userRepository.findByEmail(usernameOrEmail);
        }

        return userOptional.orElseThrow(() ->
                new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail));
    }

    @Transactional
    public User registerNewUser(RegistrationRequest request) {
        // Check if a user with this email already exists to prevent duplicates
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists. Please use a different email or log in.");
        }

        User newUser = User.builder()
                .username(request.getEmail()) // Assuming email is used as username for login
                .password(passwordEncoder.encode(request.getPassword())) // <--- Password encoding is HERE, this is correct!
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.CUSTOMER) // Assign CUSTOMER role by default for new registrations
                .registrationDate(LocalDateTime.now())
                .build();

        return userRepository.save(newUser);
    }

    // --- NEW METHOD FOR ADMIN CREATION ---
    @Transactional // <--- ADDED: This ensures database operations are handled correctly
    public User createAdminUser(String username, String email, String password, String firstName, String lastName) {
        if (userRepository.findByEmail(email).isPresent()) {
            // It's good practice to throw an error if the user already exists
            throw new IllegalArgumentException("Admin user with email " + email + " already exists.");
        }
        User adminUser = User.builder()
                .username(username) // Use the provided username for admin
                .email(email)
                .password(passwordEncoder.encode(password)) // <--- Password will be encoded
                .firstName(firstName)
                .lastName(lastName)
                .role(Role.ADMIN) // <--- Explicitly set the role to ADMIN
                .registrationDate(LocalDateTime.now())
                .build();
        return userRepository.save(adminUser);
    }
    // --- END OF NEW METHOD ---
}