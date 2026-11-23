package com.example.ticketreservation.dto;

import java.math.BigDecimal;

public record StatsResponse(

        long totalUsers,

        long totalEvents,

        long ticketsSold,

        BigDecimal totalRevenue

) {}