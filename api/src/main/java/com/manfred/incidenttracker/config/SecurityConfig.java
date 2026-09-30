package com.manfred.incidenttracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration //Tells Spring that the class contains config, not logic.
public class SecurityConfig {

    // METHOD
    // Spring hand http (a builder), the method configures it and returns http.build()
    @Bean 
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{

        // Call    x       x     config
        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(authHttpReq -> authHttpReq
            .requestMatchers("/auth/**", "/error").permitAll()
            .anyRequest().authenticated()
        );

        return http.build();

    } 

    //METHOD
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    
}
