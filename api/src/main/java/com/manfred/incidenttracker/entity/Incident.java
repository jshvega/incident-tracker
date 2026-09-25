package com.manfred.incidenttracker.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
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
@Table(name = "incidents")
public class Incident {

    // FIELDS
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", nullable = true)
    private User assigneeId;

    private Integer slaMinutes;

    @CreationTimestamp 
    private OffsetDateTime createdAt;

    @UpdateTimestamp 
    private OffsetDateTime updatedAt;

    private OffsetDateTime resolvedAt;

    private OffsetDateTime dueAt;


    // CONSTRUCTORS
    protected Incident(){
    }
    public Incident(String title, String description, Severity severity, User reporter, int slaMinutes, OffsetDateTime dueAt){
        
        this.title = title;
        this.description = description;
        this.incidentSeverity = severity;
        this.reporterId = reporter;
        this.slaMinutes = slaMinutes;
        this.dueAt = dueAt;
        
        this.incidentStatus = Status.open;

    }
    public void setAssignee(User assignee){
        this.assigneeId = assignee;
    }


    // METHODS
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
    public User getReporterId(){
        return reporterId;
    }
    public User getAssigneeId(){
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


    // SETTERS
    public void setIncidentStatus(Status incidentStatus){
        this.incidentStatus = incidentStatus;
    }
    public void setResolvedAt(OffsetDateTime resolvedAt){
        this.resolvedAt = resolvedAt;
    }
    
}
