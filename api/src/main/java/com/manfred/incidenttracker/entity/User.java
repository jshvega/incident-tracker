package com.manfred.incidenttracker.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "users")
public class User {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String passwordHash;

    @Enumerated (EnumType.STRING)
    @JdbcType (PostgreSQLEnumJdbcType.class)
    @Column (name = "user_role", nullable = false)
    private Role userRole;

    @CreationTimestamp 
    private OffsetDateTime createdAt;



    protected User(){
    }
    public User(String email, String passwordHash, Role userRole, OffsetDateTime createdAt){
        this.email = email;
        this.passwordHash = passwordHash;
        this.userRole = userRole;
        this.createdAt = createdAt;
    }

    public Long getId(){
        return id;
    }
    public String getEmail(){
        return email;
    }
    public String getPasswordHash(){
        return passwordHash;
    }
    public Role getUserRole(){
        return userRole;
    }
    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }

}
