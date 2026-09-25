package com.erdos.ticketapp.eventservice.service;

import com.erdos.ticketapp.eventservice.client.TicketClient;
import com.erdos.ticketapp.eventservice.dto.request.EventCreateRequest;
import com.erdos.ticketapp.eventservice.dto.response.EventResponse;
import com.erdos.ticketapp.eventservice.dto.response.EventTicketingInfoResponse;
import com.erdos.ticketapp.eventservice.enums.EventStatus;
import com.erdos.ticketapp.eventservice.exception.EventNotFoundException;
import com.erdos.ticketapp.eventservice.exception.EventInvalidStateException;
import com.erdos.ticketapp.eventservice.exception.InvalidIdempotencyKeyException;
import com.erdos.ticketapp.eventservice.exception.TicketSaleClosedException;
import com.erdos.ticketapp.eventservice.kafka.EventKafkaProducer;
import com.erdos.ticketapp.eventservice.mapper.EventMapper;
import com.erdos.ticketapp.eventservice.model.Event;
import com.erdos.ticketapp.eventservice.repository.EventRepository;
import com.erdos.ticketapp.eventservice.search.criteria.EventSearchCriteria;
import com.erdos.ticketapp.eventservice.search.specification.EventSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final TicketClient ticketClient;

    private final EventRepository repository;

    private final EventMapper eventMapper;

    private final EventSpecification eventSpecification;

    private final EventKafkaProducer eventKafkaProducer;

    @Transactional
    public EventResponse create(String idempotencyKey, EventCreateRequest eventCreateRequest) {
        String validatedKey = validateIdempotencyKey(idempotencyKey);

        Optional<Event> existing = repository.findByIdempotencyKey(validatedKey);

        if (existing.isPresent()) {
            validateSameCreateRequest(existing.get(), eventCreateRequest);
            return eventMapper.toResponse(existing.get());
        }

        Event event = eventMapper.toEventFromCreate(eventCreateRequest);
        event.setCreatedAt(OffsetDateTime.now());
        event.setUpdatedAt(OffsetDateTime.now());
        event.setStatus(EventStatus.DRAFT);
        event.setIdempotencyKey(validatedKey);
        Event createdEvent = repository.save(event);

        eventKafkaProducer.sendEventCreated(createdEvent.getId(), createdEvent.getName());

        return eventMapper.toResponse(createdEvent);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> search(EventSearchCriteria eventSearchCriteria, Pageable pageable) {
        return repository.findAll(eventSpecification.build(eventSearchCriteria), pageable)
                .map(eventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getById(UUID id) {
        return repository.findById(id)
                .map(eventMapper::toResponse)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    @Transactional
    public EventResponse cancel(UUID id) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        if (event.getStatus() == EventStatus.CANCELLED) {
            return eventMapper.toResponse(event);
        }

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new EventInvalidStateException(
                    "Only a published event can be cancelled. Current status: " + event.getStatus());
        }

        event.setStatus(EventStatus.CANCELLED);
        event.setUpdatedAt(OffsetDateTime.now());
        Event cancelledEvent = repository.save(event);

        eventKafkaProducer.sendEventCancelled(cancelledEvent.getId(), cancelledEvent.getName());

        return eventMapper.toResponse(cancelledEvent);
    }

    @Transactional
    public EventResponse publish(UUID id) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        if (EventStatus.PUBLISHED.equals(event.getStatus())) {
            return eventMapper.toResponse(event);
        }

        if(!EventStatus.DRAFT.equals(event.getStatus())) {
            throw new EventInvalidStateException("Only a draft event can be published. Current status: " + event.getStatus());
        }

        if(!validToPublish(event)) {
            throw new EventInvalidStateException("The Event is not valid to be published");
        }

        event.setStatus(EventStatus.PUBLISHED);
        event.setUpdatedAt(OffsetDateTime.now());
        Event publishedEvent = repository.save(event);

        eventKafkaProducer.sendEventPublished(publishedEvent.getId(), publishedEvent.getName());

        return eventMapper.toResponse(publishedEvent);
    }

    private boolean validToPublish(Event event) {
        if(StringUtils.isBlank(event.getVenueName()) || StringUtils.isBlank(event.getAddress())) {
            return false;
        }

        if(event.getCapacity() < 1) {
            return false;
        }

        if(event.getStartsAt().isBefore(OffsetDateTime.now())) {
            return false;
        }

        return !event.getStartsAt().isAfter(event.getEndsAt());
    }

    private String validateIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key header is required"
            );
        }

        String trimmedKey = key.trim();

        if (trimmedKey.length() > 100) {
            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key cannot be longer than 100 characters"
            );
        }

        return trimmedKey;
    }

    private void validateSameCreateRequest(
            Event existing,
            EventCreateRequest request
    ) {
        boolean sameRequest =
                Objects.equals(existing.getName(), request.name())
                        && Objects.equals(existing.getDescription(), request.description())
                        && Objects.equals(existing.getOrganizerId(), request.organizerId())
                        && Objects.equals(existing.getVenueName(), request.venueName())
                        && Objects.equals(existing.getAddress(), request.address())
                        && sameInstant(existing.getStartsAt(), request.startsAt())
                        && sameInstant(existing.getEndsAt(), request.endsAt())
                        && sameInstant(existing.getTicketSalesStart(), request.ticketSalesStart())
                        && sameInstant(existing.getTicketSalesEnd(), request.ticketSalesEnd())
                        && Objects.equals(existing.getCapacity(), request.capacity())
                        && sameAmount(existing.getBasePrice(), request.basePrice())
                        && Objects.equals(existing.getCurrency(), request.currency())
                        && Objects.equals(existing.getImageUrl(), request.imageUrl());

        if (!sameRequest) {
            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key was already used with a different request"
            );
        }
    }

    private boolean sameInstant(OffsetDateTime first, OffsetDateTime second) {
        if (first == null || second == null) {
            return first == second;
        }

        return first.toInstant().equals(second.toInstant());
    }

    private boolean sameAmount(BigDecimal first, BigDecimal second) {
        if (first == null || second == null) {
            return first == second;
        }

        return first.compareTo(second) == 0;
    }

    @Transactional(readOnly = true)
    public EventTicketingInfoResponse getTicketingInfo(UUID id) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        if(!EventStatus.PUBLISHED.equals(event.getStatus())) {
            throw new EventInvalidStateException("Only a published event have a ticketing info. Current stats: " + event.getStatus());
        }

        if(event.getTicketSalesStart().isAfter(OffsetDateTime.now()) || event.getTicketSalesEnd().isBefore(OffsetDateTime.now())) {
            throw new TicketSaleClosedException("The ticket sale is closed!");
        }

        return eventMapper.toTicketingInfo(event);
    }
}
