package com.example.ticketreservation.controller;

import com.example.ticketreservation.dto.StatsResponse;
import com.example.ticketreservation.dto.UserResponse;

import com.example.ticketreservation.service.AdminService;
import com.example.ticketreservation.service.AuthService;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;

    private final AdminService adminService;

    public AdminController(
            AuthService authService,
            AdminService adminService) {

        this.authService = authService;
        this.adminService = adminService;
    }

    // Get all registered users
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        List<UserResponse> users = authService.getAllUsers();

        return ResponseEntity.ok(users);
    }

    // Get system statistics
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {

        StatsResponse stats = adminService.getStats();

        return ResponseEntity.ok(stats);
    }
}