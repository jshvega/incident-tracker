package com.manfred.incidenttracker.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "events.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpEventPublisher.class);

    @Override
    public void publish(IncidentCreatedDetail detail) {
        log.info("event_publish_disabled incidentId={}", detail.incidentId());
    }
}