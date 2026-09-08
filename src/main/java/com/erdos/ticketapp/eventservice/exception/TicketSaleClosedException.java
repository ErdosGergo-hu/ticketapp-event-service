package com.erdos.ticketapp.eventservice.exception;

public class TicketSaleClosedException extends RuntimeException {

    public TicketSaleClosedException(String message) {
        super(message);
    }
}
