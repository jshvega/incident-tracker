package com.manfred.incidenttracker.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;



@Entity 
@Table (name = "comments")
public class Comment {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = true)
    private User authorId;

    private String body;

    @CreationTimestamp 
    private OffsetDateTime createdAt;


    // CONSTRUCTOR
    protected Comment(){
    }
    public Comment(Incident incident, User author, String body){
        this.incident = incident;
        this.authorId = author;
        this.body = body;
    }


    public Long getId(){
        return id;
    } 
    public Incident getIncident(){
        return incident;
    } 
    public User getAuthorId(){
        return authorId;
    } 
    public String getBody(){
        return body;
    } 
    public OffsetDateTime getCreatedAt(){
        return createdAt;
    } 
    
}
