package com.erdos.ticketapp.eventservice.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
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
        @NotNull OffsetDateTime ticketSalesStart,
        @NotNull OffsetDateTime ticketSalesEnd,
        @NotNull @Positive Integer capacity,
        @NotNull @PositiveOrZero BigDecimal basePrice,
        @NotBlank @Size(min = 3, max = 3) String currency,
        String imageUrl
) {
}