package com.erdos.ticketapp.eventservice.client.dto;

import java.util.UUID;

public record TicketDto(
        UUID id,
        String username,
        boolean enabled
) {
}