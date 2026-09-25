package com.erdos.ticketapp.eventservice.repository;

import com.erdos.ticketapp.eventservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    Optional<Event> findById(UUID id);

    Optional<Event> findByIdempotencyKey(String idempotencyKey);
}
