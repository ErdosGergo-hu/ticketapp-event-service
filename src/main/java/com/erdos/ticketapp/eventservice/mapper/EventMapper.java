package com.erdos.ticketapp.eventservice.mapper;

import com.erdos.ticketapp.eventservice.dto.request.EventCreateRequest;
import com.erdos.ticketapp.eventservice.dto.response.EventResponse;
import com.erdos.ticketapp.eventservice.dto.response.EventTicketingInfoResponse;
import com.erdos.ticketapp.eventservice.model.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper {

    EventResponse toResponse(Event event);

    Event toEventFromCreate(EventCreateRequest eventCreateRequest);

    @Mapping(target = "eventId", source = "id")
    EventTicketingInfoResponse toTicketingInfo(Event event);
}
