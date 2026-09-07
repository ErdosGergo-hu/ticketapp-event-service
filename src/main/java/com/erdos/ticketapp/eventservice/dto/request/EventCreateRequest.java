package com.erdos.ticketapp.eventservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventCreateRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull UUID organizerId,
        @NotBlank String venueName,
        @NotBlank String address,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        OffsetDateTime ticketSalesStart,
        OffsetDateTime ticketSalesEnd,
        @NotNull Integer capacity,
        String imageUrl
) {
}