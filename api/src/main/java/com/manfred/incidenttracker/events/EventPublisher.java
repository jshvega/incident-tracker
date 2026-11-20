package com.manfred.incidenttracker.events;

public interface EventPublisher {
    void publish(IncidentCreatedDetail detail);
}