package com.erdos.ticketapp.eventservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "ticket-backend",
        url = "${clients.ticket-backend.url}"
)
public interface TicketClient {

    @GetMapping("/ticket")
    String getTicket();
}