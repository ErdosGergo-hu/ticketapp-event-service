package com.erdos.ticketapp.eventservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventKafkaProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${app.kafka.notification-topic}")
    private String notificationTopic;

    public void sendTestNotification(UUID eventId, String message) {
        NotificationEvent notificationEvent = new NotificationEvent(
                eventId,
                "EVENT_TEST",
                message,
                Instant.now());

        kafkaTemplate.send(notificationTopic, eventId.toString(), notificationEvent)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Kafka test notification could not be sent for event {}", eventId, exception);
                        return;
                    }

                    log.info("Kafka test notification sent for event {} to partition {} at offset {}",
                            eventId,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }
}
