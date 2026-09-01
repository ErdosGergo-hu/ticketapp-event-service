package com.erdos.ticketapp.eventservice.search.criteria;

import lombok.Data;

import java.util.UUID;

@Data
public class EventSearchCriteria {
    private String query;
    private UUID organizedId;
    private String name;
    private String venueName;
}

