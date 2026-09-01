package com.erdos.ticketapp.eventservice.dto.response;

import com.erdos.ticketapp.eventservice.enums.EventStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String name,
        String description,
        UUID organizerId,
        String venueName,
        String address,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        OffsetDateTime ticketSalesStart,
        OffsetDateTime ticketSalesEnd,
        Integer capacity,
        EventStatus status,
        String imageUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
) {
}
