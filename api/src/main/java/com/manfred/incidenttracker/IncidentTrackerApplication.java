package com.manfred.incidenttracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class IncidentTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(IncidentTrackerApplication.class, args);
	}

}