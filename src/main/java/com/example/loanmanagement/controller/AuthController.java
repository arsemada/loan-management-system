package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.AuthenticationResponse;
import com.example.loanmanagement.dto.AuthenticationRequest;
import com.example.loanmanagement.dto.RegistrationRequest;

import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.service.JwtService;
import com.example.loanmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest request) {
        try {
            User registeredUser = userService.registerNewUser(request);

            UserDetails userDetails = userService.loadUserByUsername(registeredUser.getUsername()); // Or use registeredUser directly if it implements UserDetails
            String jwtToken = jwtService.generateToken(userDetails);


            return ResponseEntity.ok(AuthenticationResponse.builder()
                    .token(jwtToken)
                    .username(registeredUser.getUsername())
                    .role(registeredUser.getRole().name())
                    .build());
        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed: " + e.getMessage());
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthenticationRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(), // Make sure this can be either username or email as per your UserService
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();


            String jwtToken = jwtService.generateToken(userDetails);

            return ResponseEntity.ok(AuthenticationResponse.builder()
                    .token(jwtToken)
                    .username(userDetails.getUsername())
                    .role(userDetails.getAuthorities().iterator().next().getAuthority())
                    .build());
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials: User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials: " + e.getMessage());
        }
    }
}