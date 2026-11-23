package com.example.ticketreservation.controller;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;

import com.example.ticketreservation.service.EventService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {

        this.eventService = eventService;
    }

    // Get all events or search events by name
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents(
            @RequestParam(required = false) String name) {

        List<EventResponse> events;

        if (name == null || name.isBlank()) {

            events = eventService.getAllEvents();

        } else {

            events = eventService.searchEvents(name);
        }

        return ResponseEntity.ok(events);
    }

    // Get event by ID
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable String id) {

        EventResponse event = eventService.getEventById(id);

        return ResponseEntity.ok(event);
    }

    // Create a new event
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request,
            Authentication authentication) {

        EventResponse event = eventService.createEvent(
                request,
                authentication.getName()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(event);
    }

    // Update an existing event
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable String id,
            @Valid @RequestBody EventRequest request,
            Authentication authentication) {

        boolean isAdmin = isAdmin(authentication);

        EventResponse event = eventService.updateEvent(
                id,
                request,
                authentication.getName(),
                isAdmin
        );

        return ResponseEntity.ok(event);
    }

    // Delete an event (ADMIN only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable String id) {

        eventService.deleteEvent(id);

        return ResponseEntity.noContent().build();
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