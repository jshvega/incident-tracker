package com.manfred.incidenttracker.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "events.enabled", havingValue = "true")
public class EventBridgeEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventBridgeEventPublisher.class);
    static final String SOURCE = "incident-tracker.api";
    static final String DETAIL_TYPE = "IncidentCreated";

    private final EventBridgeClient client;
    private final JsonMapper jsonMapper;
    private final String busName;

    public EventBridgeEventPublisher(EventBridgeClient client, JsonMapper jsonMapper, @Value("${events.bus-name}") String busName) {
        this.client = client;
        this.jsonMapper = jsonMapper;
        this.busName = busName;
    }

    @Override
    public void publish(IncidentCreatedDetail detail) {
        try {
            String json = jsonMapper.writeValueAsString(detail);

            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .eventBusName(busName)
                    .source(SOURCE)
                    .detailType(DETAIL_TYPE)
                    .detail(json)
                    .build();

            PutEventsRequest request = PutEventsRequest.builder()
                    .entries(entry)
                    .build();

            PutEventsResponse response = client.putEvents(request);

            Integer failed = response.failedEntryCount();

            if (failed != null && failed > 0) {
                PutEventsResultEntry result = response.entries().get(0);

                log.error(
                        "event_publish_failed incidentId={} errorCode={} error={}",
                        detail.incidentId(),
                        result.errorCode(),
                        result.errorMessage()
                );
                return;
            }

            log.info(
                    "event_published incidentId={} eventId={}",
                    detail.incidentId(),
                    response.entries().get(0).eventId()
            );

        } catch (RuntimeException e) {
            log.error(
                    "event_publish_failed incidentId={} error={}",
                    detail.incidentId(),
                    e.getMessage(),
                    e
            );
        }
    }
}