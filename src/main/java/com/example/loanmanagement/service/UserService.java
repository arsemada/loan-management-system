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
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        Optional<User> userOptional = userRepository.findByUsername(usernameOrEmail);

        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(usernameOrEmail);
        }

        return userOptional.orElseThrow(() ->
                new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail));
    }

    @Transactional
    public User registerNewUser(RegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists. Please use a different email or log in.");
        }

        User newUser = User.builder()
                .username(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.CUSTOMER)
                .registrationDate(LocalDateTime.now())
                .build();

        return userRepository.save(newUser);
    }

    @Transactional
    public User createAdminUser(String username, String email, String password, String firstName, String lastName) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Admin user with email " + email + " already exists.");
        }
        User adminUser = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .role(Role.ADMIN)
                .registrationDate(LocalDateTime.now())
                .build();
        return userRepository.save(adminUser);
    }
}