package com.erdos.ticketapp.eventservice.controller;

import com.erdos.ticketapp.eventservice.dto.request.EventCreateRequest;
import com.erdos.ticketapp.eventservice.dto.response.EventTicketingInfoResponse;
import com.erdos.ticketapp.eventservice.kafka.EventKafkaProducer;
import com.erdos.ticketapp.eventservice.dto.response.EventResponse;
import com.erdos.ticketapp.eventservice.search.criteria.EventSearchCriteria;
import com.erdos.ticketapp.eventservice.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventKafkaProducer eventKafkaProducer;

    @PostMapping
    public ResponseEntity<EventResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid EventCreateRequest eventCreateRequest) {
        EventResponse eventResponse = eventService.create(idempotencyKey, eventCreateRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(eventResponse.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(eventResponse);
    }

    @GetMapping("/search")
    public Page<EventResponse> getPageableAuctions(
            EventSearchCriteria eventSearchCriteria,
            Pageable pageable) {
        return eventService.search(eventSearchCriteria, pageable);
    }

    @GetMapping("/{id}")
    public EventResponse getById(@PathVariable UUID id) {
        return eventService.getById(id);
    }

    @PostMapping("/{id}/cancel")
    public EventResponse cancel(@PathVariable UUID id) {
        return eventService.cancel(id);
    }

    @PostMapping("/{id}/publish")
    public EventResponse publish(@PathVariable UUID id) {
        return eventService.publish(id);
    }

    @GetMapping("/{id}/ticketing-info")
    public EventTicketingInfoResponse ticketingInfo(@PathVariable UUID id) {
        return eventService.getTicketingInfo(id);
    }

    // PUT /events/{id}Only DRAFT events can be edited

    @PostMapping("/{eventId}/notification-test")
    public void sendTestNotification(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "Teszt értesítés az event-service-ből") String message) {
        eventKafkaProducer.sendTestNotification(eventId, message);
    }
}
