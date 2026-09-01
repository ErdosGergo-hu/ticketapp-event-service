package com.erdos.ticketapp.eventservice.service;

import com.erdos.ticketapp.eventservice.client.TicketClient;
import com.erdos.ticketapp.eventservice.dto.response.EventResponse;
import com.erdos.ticketapp.eventservice.mapper.EventMapper;
import com.erdos.ticketapp.eventservice.repository.EventRepository;
import com.erdos.ticketapp.eventservice.search.criteria.EventSearchCriteria;
import com.erdos.ticketapp.eventservice.search.specification.EventSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

    private final TicketClient ticketClient;

    private final EventRepository repository;

    private final EventMapper eventMapper;

    private final EventSpecification eventSpecification;

    public String getTicketHello() {
        return ticketClient.getTicket();
    }

    public Page<EventResponse> search(EventSearchCriteria eventSearchCriteria, Pageable pageable) {
        return repository.findAll(eventSpecification.build(eventSearchCriteria), pageable)
                .map(eventMapper::toResponse);
    }
}
