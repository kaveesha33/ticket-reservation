package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.AuthResponse;
import com.example.ticketreservation.dto.LoginRequest;
import com.example.ticketreservation.dto.RegisterRequest;
import com.example.ticketreservation.dto.UserResponse;

import com.example.ticketreservation.exception.ApiException;

import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Role;

import com.example.ticketreservation.repository.UserRepository;

import com.example.ticketreservation.security.JwtService;

import org.springframework.http.HttpStatus;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // Register a new customer
    public UserResponse register(RegisterRequest request) {

        // Public registration is restricted to CUSTOMER
        if (request.role() != Role.CUSTOMER) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Public registration is only allowed for customers"
            );
        }

        // Check whether username already exists
        if (userRepository.existsByUsername(request.username())) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        // Create user
        AppUser user = new AppUser();

        user.setUsername(request.username().trim());

        // Hash password using BCrypt
        user.setPassword(
                passwordEncoder.encode(request.password())
        );

        user.setFullName(request.fullName().trim());

        user.setRole(Role.CUSTOMER);

        // Save user in MongoDB
        AppUser savedUser = userRepository.save(user);

        return mapToUserResponse(savedUser);
    }

    // Authenticate user and generate JWT
    public AuthResponse login(LoginRequest request) {

        AppUser user = userRepository.findByUsername(
                request.username()
        ).orElseThrow(() ->
                new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid username or password"
                )
        );

        // Verify password
        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword())) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }

        // Build Spring Security UserDetails
        UserDetails userDetails = User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        // Generate JWT token
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                user.getUsername(),
                user.getFullName(),
                user.getRole()
        );
    }

    // Get currently authenticated user
    public UserResponse getMe(String username) {

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        return mapToUserResponse(user);
    }

    // Get all registered users
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    // Convert AppUser to UserResponse
    private UserResponse mapToUserResponse(AppUser user) {

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole()
        );
    }
}