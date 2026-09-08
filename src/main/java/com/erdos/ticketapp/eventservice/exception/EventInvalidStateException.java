package com.erdos.ticketapp.eventservice.exception;

public class EventInvalidStateException extends RuntimeException {

    public EventInvalidStateException(String message) {
        super(message);
    }
}
