package com.manfred.incidenttracker.events;

import java.time.Instant;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IncidentEventListener {

    private static final int SCHEMA_VERSION = 1;

    private final EventPublisher eventPublisher; // Spring injects whichever implementation is active

    public IncidentEventListener(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IncidentCreated event) {
        IncidentCreatedDetail detail = new IncidentCreatedDetail(SCHEMA_VERSION, event.incidentId(), event.title(), event.severity().name(), event.slaMinutes(),  toInstant(event.dueAt()), event.reporterId(), toInstant(event.createdAt()));
        eventPublisher.publish(detail);
    }

    private Instant toInstant(OffsetDateTime t) {
        return t == null ? null : t.toInstant();
    }
}