package com.manfred.incidenttracker.entity;

public enum Severity {
    low, medium, high, critical;
}

/* 
NOTE ON LOWERCASE:
The Java enum is a mirror of a Postgres enum type that two languages read, so it matches the database exactly rather than imposing Java's naming convention on shared state.
*/