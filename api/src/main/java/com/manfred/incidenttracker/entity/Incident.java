package com.manfred.incidenttracker.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private Severity incidentSeverity;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private Status incidentStatus;

    private Long reporterId;

    private Long assigneeId;

    private Integer slaMinutes;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private OffsetDateTime resolvedAt;

    private OffsetDateTime dueAt;


    protected Incident(){
    }
    public Long getId(){
        return id;
    }
    public String getTitle(){
        return title;
    }
    public String getDescription(){
        return description;
    }
    public Severity getIncidentSeverity(){
        return incidentSeverity;
    }
    public Status getIncidentStatus(){
        return incidentStatus;
    }
    public Long getReporterId(){
        return reporterId;
    }
    public Long getAssigneeId(){
        return assigneeId;
    }
    public Integer getSlaMinutes(){
        return slaMinutes;
    }
    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }
    public OffsetDateTime getUpdatedAt(){
        return updatedAt;
    }
    public OffsetDateTime getResolvedAt(){
        return resolvedAt;
    }
    public OffsetDateTime getDueAt(){
        return dueAt;
    }
    
}
