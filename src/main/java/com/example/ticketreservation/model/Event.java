package com.example.ticketreservation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    private String id;

    private String name;

    private String description;

    private String venue;

    private LocalDateTime eventDate;

    private int totalSeats;

    private int availableSeats;

    private BigDecimal ticketPrice;

    private String organizerUsername;
}