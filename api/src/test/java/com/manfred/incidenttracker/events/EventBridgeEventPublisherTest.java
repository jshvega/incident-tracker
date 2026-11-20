package com.manfred.incidenttracker.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
public class EventBridgeEventPublisherTest {

    @Mock
    private EventBridgeClient client;

    private EventBridgeEventPublisher publisher;

    private final IncidentCreatedDetail detail = new IncidentCreatedDetail(
        1, 42L, "DB down", "critical", 30,
        Instant.parse("2026-11-19T22:00:00Z"), 7L, Instant.parse("2026-11-19T21:30:00Z"));

    @BeforeEach
    void setUp() {
        publisher = new EventBridgeEventPublisher(client, JsonMapper.builder().build(), "incident-tracker");
    }

    @Test
    void publishesEntryWithContract() {

        /* ------------- ARRANGE ------------- */

        PutEventsResponse response = PutEventsResponse.builder()
                .failedEntryCount(0)
                .entries(PutEventsResultEntry.builder()
                        .eventId("e-1")
                        .build())
                .build();

        when(client.putEvents(any(PutEventsRequest.class))).thenReturn(response);


        /* ------------- ACT ------------- */

        publisher.publish(detail);


        /* ------------- ASSERT / VERIFY ------------- */

        ArgumentCaptor<PutEventsRequest> captor =
                ArgumentCaptor.forClass(PutEventsRequest.class);

        verify(client).putEvents(captor.capture());

        PutEventsRequestEntry entry = captor.getValue().entries().get(0);

        assertEquals("incident-tracker", entry.eventBusName());

        // Replace these with the expected values from your publisher constants.
        assertEquals(EventBridgeEventPublisher.SOURCE, entry.source());
        assertEquals(EventBridgeEventPublisher.DETAIL_TYPE, entry.detailType());

        assertTrue(entry.detail().contains("\"severity\":\"critical\""));
        assertTrue(entry.detail().contains("\"schemaVersion\":1"));
    }


    @Test
    void failedEntryDoesNotThrow() {

        /* ------------- ARRANGE ------------- */

        PutEventsResponse response = PutEventsResponse.builder()
                .failedEntryCount(1)
                .entries(PutEventsResultEntry.builder()
                        .errorCode("AccessDeniedException")
                        .errorMessage("denied")
                        .build())
                .build();

        when(client.putEvents(any(PutEventsRequest.class))).thenReturn(response);


        /* ------------- ACT / ASSERT ------------- */

        assertDoesNotThrow(() -> publisher.publish(detail));

    }


    @Test
    void sdkExceptionDoesNotThrow() {

        /* ------------- ARRANGE ------------- */

        when(client.putEvents(any(PutEventsRequest.class)))
                .thenThrow(SdkException.builder()
                        .message("boom")
                        .build());


        /* ------------- ACT / ASSERT ------------- */

        assertDoesNotThrow(() -> publisher.publish(detail));

    }
}