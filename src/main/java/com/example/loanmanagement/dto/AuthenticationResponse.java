package com.example.loanmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor // Lombok: Generates a constructor with all arguments
public class AuthenticationResponse {
    private String token; // The JWT token
    private String username; // Optionally include username or other user details
    private String role;     // Optionally include role for frontend
}