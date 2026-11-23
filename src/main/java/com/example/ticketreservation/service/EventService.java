package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;

import com.example.ticketreservation.exception.ApiException;

import com.example.ticketreservation.model.Event;

import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;

    private final ReservationRepository reservationRepository;

    private final MongoTemplate mongoTemplate;

    public EventService(
            EventRepository eventRepository,
            ReservationRepository reservationRepository,
            MongoTemplate mongoTemplate) {

        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.mongoTemplate = mongoTemplate;
    }

    // Create a new event
    public EventResponse createEvent(
            EventRequest request,
            String organizerUsername) {

        Event event = new Event();

        event.setName(request.name());

        event.setDescription(request.description());

        event.setVenue(request.venue());

        event.setEventDate(request.eventDate());

        event.setTotalSeats(request.totalSeats());

        // Initially all seats are available
        event.setAvailableSeats(request.totalSeats());

        event.setTicketPrice(request.ticketPrice());

        event.setOrganizerUsername(organizerUsername);

        Event savedEvent = eventRepository.save(event);

        return mapToEventResponse(savedEvent);
    }

    // Get all events
    public List<EventResponse> getAllEvents() {

        return eventRepository.findAll()
                .stream()
                .map(this::mapToEventResponse)
                .toList();
    }

    // Get event by ID
    public EventResponse getEventById(String id) {

        Event event = findEventOrThrow(id);

        return mapToEventResponse(event);
    }

    // Search events by name
    public List<EventResponse> searchEvents(String name) {

        if (name == null || name.isBlank()) {
            return getAllEvents();
        }

        return eventRepository
                .findByNameContainingIgnoreCase(name.trim())
                .stream()
                .map(this::mapToEventResponse)
                .toList();
    }

    // Update an existing event
    public EventResponse updateEvent(
            String id,
            EventRequest request,
            String username,
            boolean isAdmin) {

        Event event = findEventOrThrow(id);

        // Check event ownership
        if (!isAdmin &&
                !event.getOrganizerUsername().equals(username)) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "You cannot update another organizer's event"
            );
        }

        // Calculate seats already booked
        int bookedSeats =
                event.getTotalSeats() - event.getAvailableSeats();

        // Prevent reducing total seats below booked seats
        if (request.totalSeats() < bookedSeats) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Total seats cannot be less than already booked seats"
            );
        }

        event.setName(request.name());

        event.setDescription(request.description());

        event.setVenue(request.venue());

        event.setEventDate(request.eventDate());

        event.setTotalSeats(request.totalSeats());

        event.setAvailableSeats(
                request.totalSeats() - bookedSeats
        );

        event.setTicketPrice(request.ticketPrice());

        Event updatedEvent = eventRepository.save(event);

        return mapToEventResponse(updatedEvent);
    }

    // Delete an event
    public void deleteEvent(String id) {

        Event event = findEventOrThrow(id);

        // Prevent deleting events with reservation records
        if (reservationRepository.existsByEventId(id)) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Cannot delete an event that has reservations"
            );
        }

        eventRepository.delete(event);
    }

    // Atomically reserve seats
    public Event reserveSeats(String eventId, int seats) {

        if (seats < 1 || seats > 10) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation must be between 1 and 10 seats"
            );
        }

        /*
         * Find the event only if enough seats are available.
         *
         * MongoDB performs the seat decrement atomically.
         */

        Query query = new Query(
                Criteria.where("_id").is(eventId)
                        .and("availableSeats").gte(seats)
        );

        Update update = new Update()
                .inc("availableSeats", -seats);

        Event updatedEvent = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Event.class
        );

        if (updatedEvent == null) {

            if (!eventRepository.existsById(eventId)) {

                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                );
            }

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Not enough available seats"
            );
        }

        return updatedEvent;
    }

    // Release seats when reservation is cancelled
    public void releaseSeats(String eventId, int seats) {

        if (seats < 1) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid number of seats"
            );
        }

        Query query = new Query(
                Criteria.where("_id").is(eventId)
        );

        Update update = new Update()
                .inc("availableSeats", seats);

        var result = mongoTemplate.updateFirst(
                query,
                update,
                Event.class
        );

        if (result.getMatchedCount() == 0) {

            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Event not found"
            );
        }
    }

    // Find event or throw 404
    private Event findEventOrThrow(String id) {

        return eventRepository.findById(id)
                .orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Event not found"
                        )
                );
    }

    // Convert Event to EventResponse
    private EventResponse mapToEventResponse(Event event) {

        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getVenue(),
                event.getEventDate(),
                event.getTotalSeats(),
                event.getAvailableSeats(),
                event.getTicketPrice(),
                event.getOrganizerUsername()
        );
    }
}