package com.erdos.ticketapp.eventservice.search.specification;

import com.erdos.ticketapp.eventservice.model.Event;
import com.erdos.ticketapp.eventservice.search.criteria.EventSearchCriteria;
import org.springframework.stereotype.Component;

@Component
public class EventSpecification extends AbstractSpecification<EventSearchCriteria, Event> {
}
