package com.erdos.ticketapp.eventservice.controller;

import com.erdos.ticketapp.eventservice.dto.response.EventResponse;
import com.erdos.ticketapp.eventservice.search.criteria.EventSearchCriteria;
import com.erdos.ticketapp.eventservice.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/search")
    public Page<EventResponse> getPageableAuctions(
            EventSearchCriteria eventSearchCriteria,
            Pageable pageable) {
        return eventService.search(eventSearchCriteria, pageable);
    }

    @GetMapping
    public String hello() {
        return eventService.getTicketHello();
    }
}
