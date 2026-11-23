package com.example.ticketreservation.dto;

import com.example.ticketreservation.model.ReservationStatus;

import java.math.BigDecimal;

public record ReservationResponse(

        String id,

        String reservationCode,

        String customerUsername,

        String eventId,

        int seats,

        BigDecimal amount,

        ReservationStatus status

) {}