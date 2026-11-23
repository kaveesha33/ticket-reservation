package com.example.ticketreservation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document(collection = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    private String id;

    @Indexed(unique = true)
    private String reservationCode;

    private String customerUsername;

    private String eventId;

    private int seats;

    private BigDecimal amount;

    private ReservationStatus status;
}