package com.manfred.incidenttracker.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService){
        this.jwtService = jwtService;
    }


    /*
    Runs once per request, before authorization.
    If there's a valid "Bearer <token>", put the AuthUser in the SecurityContext.
    If the token is missing or invalid, leave the context empty.
    Always pass the request on: deciding allow or deny is the authorization rules' job.
    */
    @Override 
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if(header == null || !header.startsWith("Bearer ")){
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring("Bearer ".length());

        try{
            AuthUser authUser = jwtService.parse(token);
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + authUser.role().name().toUpperCase());
            UsernamePasswordAuthenticationToken threeArg = new UsernamePasswordAuthenticationToken(authUser, null, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(threeArg);
        }
        catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);

    }

}
