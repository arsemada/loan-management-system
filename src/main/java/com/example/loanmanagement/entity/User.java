package com.example.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "_user") // Good practice to avoid SQL keyword conflict with 'user'
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username; // Renamed from email as username is used for login
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private LocalDateTime registrationDate;

    @Enumerated(EnumType.STRING)
    private Role role; // Your Role enum (e.g., CUSTOMER, ADMIN)

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // THIS IS THE CRITICAL CHANGE:
        // Spring Security's hasRole() expects roles to be prefixed with "ROLE_"
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username; // This method should return the field used for login (e.g., username or email)
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Return true unless you implement account expiration logic
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Return true unless you implement account locking logic
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Return true unless you implement credential expiration logic
    }

    @Override
    public boolean isEnabled() {
        return true; // Return true unless you implement account enabling/disabling logic
    }
}