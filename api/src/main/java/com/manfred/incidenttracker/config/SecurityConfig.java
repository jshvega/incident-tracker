package com.manfred.incidenttracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.manfred.incidenttracker.security.JwtAccessDeniedHandler;
import com.manfred.incidenttracker.security.JwtAuthenticationEntryPoint;
import com.manfred.incidenttracker.security.JwtAuthenticationFilter;
import com.manfred.incidenttracker.security.JwtService;
import org.springframework.http.HttpMethod;

@Configuration //Tells Spring that the class contains config, not logic.
public class SecurityConfig {

    // FIELDS
    private final JwtService jwtService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    // CONSTRUCTOR
    public SecurityConfig(JwtService jwtService, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint, JwtAccessDeniedHandler jwtAccessDeniedHandler){
        this.jwtService = jwtService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }


    // METHOD
    // Spring hand http (a builder), the method configures it and returns http.build()
    @Bean 
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{

        // Call    x       x     config
        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(authHttpReq -> authHttpReq
            .requestMatchers("/auth/**", "/error").permitAll()
            .requestMatchers(HttpMethod.GET, "/actuator/health/liveness").permitAll()
            .requestMatchers(HttpMethod.DELETE, "/incidents/**").hasRole("ADMIN")
            .requestMatchers("/users/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        );

        http.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        http.exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint).accessDeniedHandler(jwtAccessDeniedHandler));

        return http.build();

    } 

    //METHOD
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    
}
