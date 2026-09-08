package com.erdos.ticketapp.eventservice.dto.response;

import com.erdos.ticketapp.eventservice.enums.EventStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EventTicketingInfoResponse(
        UUID eventId,
        EventStatus status,
        OffsetDateTime ticketSalesStart,
        OffsetDateTime ticketSalesEnd,
        BigDecimal basePrice,
        String currency
) {
}
