package com.manfred.incidenttracker.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
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
@Table (name = "status_history")
public class StatusHistory {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incidentId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private Status fromStatus;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private Status toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = true)
    private User changedBy;

    @CreationTimestamp 
    private OffsetDateTime changedAt;



    protected StatusHistory(){
    }
    public Long getId(){
        return id;
    }
    public Incident getIncidentId(){
        return incidentId;
    }
    public Status getFromStatus(){
        return fromStatus;
    }
    public Status getToStatus(){
        return toStatus;
    }
    public User getChangedBy(){
        return changedBy;
    }
    public OffsetDateTime getChangedAt(){
        return changedAt;
    }

}
