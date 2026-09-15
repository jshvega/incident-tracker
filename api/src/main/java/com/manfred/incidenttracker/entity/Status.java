package com.manfred.incidenttracker.entity;

public enum Status {
    open, investigating, resolved, closed;
}

/* 
NOTE ON LOWERCASE:
The Java enum is a mirror of a Postgres enum type that two languages read, so it matches the database exactly rather than imposing Java's naming convention on shared state.
*/