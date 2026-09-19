package com.manfred.incidenttracker.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity 
@Table (name = "escalations")
public class Escalation {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incidentId;

    private String reason;

    private OffsetDateTime escalatedAt;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private Severity previousSeverity;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private Severity newSeverity;



    protected Escalation() {
    }
    public Long getId(){
        return id;
    }
    public Incident getIncidentId(){
        return incidentId;
    }
    public String getReason(){
        return reason;
    }
    public OffsetDateTime getEscalatedAt(){
        return escalatedAt;
    }
    public Severity getPreviousSeverity(){
        return previousSeverity;
    }
    public Severity getNewSeverity(){
        return newSeverity;
    }

}
