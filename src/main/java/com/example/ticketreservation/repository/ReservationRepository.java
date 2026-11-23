package com.example.ticketreservation.repository;

import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {

    List<Reservation> findByCustomerUsername(String customerUsername);

    List<Reservation> findByEventId(String eventId);

    List<Reservation> findByStatus(ReservationStatus status);

    boolean existsByEventId(String eventId);

    long countByStatus(ReservationStatus status);
}