package com.manfred.incidenttracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class IncidentTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(IncidentTrackerApplication.class, args);
	}

}