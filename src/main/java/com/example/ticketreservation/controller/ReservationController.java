package com.example.ticketreservation.controller;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;

import com.example.ticketreservation.service.ReservationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(
            ReservationService reservationService) {

        this.reservationService = reservationService;
    }

    // Book tickets (CUSTOMER only)
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReservationResponse> bookReservation(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {

        ReservationResponse reservation =
                reservationService.bookReservation(
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservation);
    }

    // Get reservations belonging to current customer
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            Authentication authentication) {

        List<ReservationResponse> reservations =
                reservationService.getMyReservations(
                        authentication.getName()
                );

        return ResponseEntity.ok(reservations);
    }

    // Get all reservations (ADMIN)
    // Get reservations for organizer's events (ORGANIZER)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<List<ReservationResponse>> getReservations(
            Authentication authentication) {

        List<ReservationResponse> reservations;

        if (isAdmin(authentication)) {

            // Administrator can view all reservations
            reservations = reservationService.getAllReservations();

        } else {

            // Organizer can only view reservations for their events
            reservations = reservationService
                    .getReservationsForOrganizer(
                            authentication.getName()
                    );
        }

        return ResponseEntity.ok(reservations);
    }

    // Cancel a reservation (CUSTOMER owner or ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable String id,
            Authentication authentication) {

        boolean isAdmin = isAdmin(authentication);

        ReservationResponse reservation =
                reservationService.cancelReservation(
                        id,
                        authentication.getName(),
                        isAdmin
                );

        return ResponseEntity.ok(reservation);
    }

    // Check whether authenticated user is ADMIN
    private boolean isAdmin(Authentication authentication) {

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                );
    }
}