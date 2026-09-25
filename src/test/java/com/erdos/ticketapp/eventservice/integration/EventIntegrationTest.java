package com.erdos.ticketapp.eventservice.integration;

import com.erdos.ticketapp.eventservice.enums.EventStatus;
import com.erdos.ticketapp.eventservice.kafka.EventKafkaProducer;
import com.erdos.ticketapp.eventservice.model.Event;
import com.erdos.ticketapp.eventservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @MockitoBean
    private EventKafkaProducer eventKafkaProducer;

    @BeforeEach
    void cleanDatabase() {
        eventRepository.deleteAll();
    }

    private Event saveTestEvent() {
        return saveTestEvent(EventStatus.DRAFT);
    }

    private Event saveTestEvent(EventStatus status) {
        Event event = new Event();

        event.setName("Karácsonyi Kézműves Vásár");
        event.setDescription(
                "Ünnepi vásár helyi kézművesekkel és családi programokkal."
        );
        event.setOrganizerId(UUID.fromString(
                "20000000-0000-0000-0000-000000000003"
        ));
        event.setVenueName("Vörösmarty tér");
        event.setAddress("1051 Budapest, Vörösmarty tér");
        event.setStartsAt(
                OffsetDateTime.parse("2026-12-05T10:00:00+01:00")
        );
        event.setEndsAt(
                OffsetDateTime.parse("2026-12-05T20:00:00+01:00")
        );
        event.setTicketSalesStart(
                OffsetDateTime.parse("2026-08-01T09:00:00+02:00")
        );
        event.setTicketSalesEnd(
                OffsetDateTime.parse("2026-12-04T23:59:00+01:00")
        );
        event.setBasePrice(new BigDecimal("14.99"));
        event.setCurrency("EUR");
        event.setCapacity(1000);
        event.setStatus(status);
        event.setImageUrl(
                "https://example.com/images/christmas-market.jpg"
        );
        event.setCreatedAt(
                OffsetDateTime.parse("2026-09-01T10:15:00+02:00")
        );
        event.setUpdatedAt(
                OffsetDateTime.parse("2026-09-01T10:15:00+02:00")
        );
        event.setIdempotencyKey(UUID.randomUUID().toString());

        return eventRepository.saveAndFlush(event);
    }

    @Test
    void testShouldCreateEventWithValidRequestBody() throws Exception {
        String requestBody = """
                {
                  "name": "Karácsonyi Kézműves Vásár",
                  "description": "Ünnepi vásár helyi kézművesekkel és családi programokkal.",
                  "organizerId": "550e8400-e29b-41d4-a716-446655440000",
                  "venueName": "Budapest Congress Center",
                  "address": "Budapest, Jagelló út 1-3",
                  "startsAt": "2099-10-15T09:00:00+02:00",
                  "endsAt": "2099-10-15T18:00:00+02:00",
                  "ticketSalesStart": "2099-08-01T08:00:00+02:00",
                  "ticketSalesEnd": "2099-10-14T23:59:00+02:00",
                  "capacity": 500,
                  "basePrice": 14990,
                  "currency": "HUF",
                  "imageUrl": "https://example.com/java-conference.jpg"
                }
                """;

        mockMvc.perform(post("/events")
                        .header("Idempotency-Key", "test-event-create-valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Karácsonyi Kézműves Vásár"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(post("/events")
                        .header("Idempotency-Key", "test-event-create-valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Karácsonyi Kézműves Vásár"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        List<Event> savedEvents = eventRepository.findAll();

        assertThat(savedEvents).hasSize(1);
        assertThat(savedEvents.getFirst().getName())
                .isEqualTo("Karácsonyi Kézműves Vásár");
        assertThat(savedEvents.getFirst().getStatus())
                .isEqualTo(EventStatus.DRAFT);
        verify(eventKafkaProducer).sendEventCreated(
                savedEvents.getFirst().getId(),
                savedEvents.getFirst().getName());
    }

    @Test
    void testShouldNotCreateEventWithInvalidRequestBody() throws Exception {
        String requestBody = """
                {
                  "name": "Karácsonyi Kézműves Vásár",
                  "description": "Ünnepi vásár helyi kézművesekkel és családi programokkal.",
                  "organizerId": "550e8400-e29b-41d4-a716-446655440000",
                  "address": "Budapest, Jagelló út 1-3",
                  "startsAt": "2099-10-15T09:00:00+02:00",
                  "endsAt": "2099-10-15T18:00:00+02:00",
                  "ticketSalesStart": "2099-08-01T08:00:00+02:00",
                  "ticketSalesEnd": "2099-10-14T23:59:00+02:00",
                  "capacity": 500,
                  "basePrice": 14990,
                  "currency": "HUF",
                  "imageUrl": "https://example.com/java-conference.jpg"
                }
                """;

        mockMvc.perform(post("/events")
                        .header("Idempotency-Key", "test-event-create-invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testShouldReturnEventWithValidUuid() throws Exception {
        Event event = saveTestEvent();

        mockMvc.perform(get("/events/{id}", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.getId().toString()))
                .andExpect(jsonPath("$.status").value(event.getStatus().toString()))
                .andExpect(jsonPath("$.venueName").value(event.getVenueName()));
    }

    @Test
    void testShouldNotReturnEventWithInvalidUuid() throws Exception {
        saveTestEvent();

        mockMvc.perform(get("/events/{id}", "10000000-0000-0000-0000-000000000002"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDraftEventCanBePublished() throws Exception {
        Event event = saveTestEvent();

        mockMvc.perform(post("/events/{id}/publish", event.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(EventStatus.PUBLISHED.toString()));

        List<Event> savedEvents = eventRepository.findAll();

        assertThat(savedEvents).hasSize(1);
        assertThat(savedEvents.getFirst().getName())
                .isEqualTo("Karácsonyi Kézműves Vásár");
        assertThat(savedEvents.getFirst().getStatus())
                .isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void testPublishedEventCanBePublishedAgainIdempotently() throws Exception {
        Event event = saveTestEvent(EventStatus.PUBLISHED);

        mockMvc.perform(post("/events/{id}/publish", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.getId().toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        Event unchangedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(unchangedEvent.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verifyNoInteractions(eventKafkaProducer);
    }

    @Test
    void testPublishedEventCanBeCancelled() throws Exception {
        Event event = saveTestEvent(EventStatus.PUBLISHED);

        mockMvc.perform(post("/events/{id}/cancel", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.getId().toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Event cancelledEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(cancelledEvent.getStatus()).isEqualTo(EventStatus.CANCELLED);
        verify(eventKafkaProducer)
                .sendEventCancelled(cancelledEvent.getId(), cancelledEvent.getName());
    }

    @Test
    void testDraftEventCannotBeCancelled() throws Exception {
        Event event = saveTestEvent();

        mockMvc.perform(post("/events/{id}/cancel", event.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid event state"))
                .andExpect(jsonPath("$.detail")
                        .value("Only a published event can be cancelled. Current status: DRAFT"));

        Event unchangedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(unchangedEvent.getStatus()).isEqualTo(EventStatus.DRAFT);
    }
}
