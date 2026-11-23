package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;

import com.example.ticketreservation.exception.ApiException;

import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;

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
import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;

    private final EventRepository eventRepository;

    private final EventService eventService;

    private final MongoTemplate mongoTemplate;

    public ReservationService(
            ReservationRepository reservationRepository,
            EventRepository eventRepository,
            EventService eventService,
            MongoTemplate mongoTemplate) {

        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.mongoTemplate = mongoTemplate;
    }

    // Book tickets
    public ReservationResponse bookReservation(
            ReservationRequest request,
            String customerUsername) {

        // Atomically reserve seats
        Event event = eventService.reserveSeats(
                request.eventId(),
                request.seats()
        );

        // Calculate total price
        var amount = event.getTicketPrice()
                .multiply(
                        java.math.BigDecimal.valueOf(request.seats())
                );

        // Create reservation
        Reservation reservation = new Reservation();

        reservation.setReservationCode(generateReservationCode());

        reservation.setCustomerUsername(customerUsername);

        reservation.setEventId(event.getId());

        reservation.setSeats(request.seats());

        reservation.setAmount(amount);

        reservation.setStatus(ReservationStatus.CONFIRMED);

        try {

            Reservation savedReservation =
                    reservationRepository.save(reservation);

            return mapToReservationResponse(savedReservation);

        } catch (RuntimeException ex) {

            /*
             * If reservation saving fails after seats were taken,
             * attempt to release those seats.
             */

            try {

                eventService.releaseSeats(
                        event.getId(),
                        request.seats()
                );

            } catch (RuntimeException rollbackException) {

                ex.addSuppressed(rollbackException);
            }

            throw ex;
        }
    }

    // Get reservations belonging to a customer
    public List<ReservationResponse> getMyReservations(
            String customerUsername) {

        return reservationRepository
                .findByCustomerUsername(customerUsername)
                .stream()
                .map(this::mapToReservationResponse)
                .toList();
    }

    // Get all reservations (ADMIN)
    public List<ReservationResponse> getAllReservations() {

        return reservationRepository.findAll()
                .stream()
                .map(this::mapToReservationResponse)
                .toList();
    }

    // Get reservations for events belonging to an organizer
    public List<ReservationResponse> getReservationsForOrganizer(
            String organizerUsername) {

        return eventRepository
                .findByOrganizerUsername(organizerUsername)
                .stream()
                .flatMap(event ->
                        reservationRepository
                                .findByEventId(event.getId())
                                .stream()
                )
                .map(this::mapToReservationResponse)
                .toList();
    }

    // Cancel reservation
    public ReservationResponse cancelReservation(
            String reservationId,
            String username,
            boolean isAdmin) {

        Reservation existing = reservationRepository
                .findById(reservationId)
                .orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Reservation not found"
                        )
                );

        // Verify ownership
        if (!isAdmin &&
                !existing.getCustomerUsername().equals(username)) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "You cannot cancel another customer's reservation"
            );
        }

        if (existing.getStatus() == ReservationStatus.CANCELLED) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Reservation has already been cancelled"
            );
        }

        /*
         * Atomically change the reservation status.
         * This prevents two cancellation requests from releasing
         * the same seats twice.
         */

        Query query = new Query(
                Criteria.where("_id").is(reservationId)
                        .and("status").is(ReservationStatus.CONFIRMED)
        );

        if (!isAdmin) {

            query.addCriteria(
                    Criteria.where("customerUsername").is(username)
            );
        }

        Update update = new Update()
                .set("status", ReservationStatus.CANCELLED);

        Reservation cancelledReservation =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        FindAndModifyOptions.options().returnNew(true),
                        Reservation.class
                );

        if (cancelledReservation == null) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Reservation could not be cancelled because its status has changed"
            );
        }

        try {

            // Release previously reserved seats
            eventService.releaseSeats(
                    cancelledReservation.getEventId(),
                    cancelledReservation.getSeats()
            );

        } catch (RuntimeException ex) {

            // Attempt to restore confirmed status if seat release fails
            Query rollbackQuery = new Query(
                    Criteria.where("_id").is(reservationId)
                            .and("status").is(ReservationStatus.CANCELLED)
            );

            Update rollbackUpdate = new Update()
                    .set("status", ReservationStatus.CONFIRMED);

            mongoTemplate.updateFirst(
                    rollbackQuery,
                    rollbackUpdate,
                    Reservation.class
            );

            throw ex;
        }

        return mapToReservationResponse(cancelledReservation);
    }

    // Generate unique reservation code
    private String generateReservationCode() {

        return "TKT-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }

    // Convert Reservation to ReservationResponse
    private ReservationResponse mapToReservationResponse(
            Reservation reservation) {

        return new ReservationResponse(
                reservation.getId(),
                reservation.getReservationCode(),
                reservation.getCustomerUsername(),
                reservation.getEventId(),
                reservation.getSeats(),
                reservation.getAmount(),
                reservation.getStatus()
        );
    }
}