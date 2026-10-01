package com.manfred.incidenttracker.security;

import java.util.Date;
import java.time.*;
import java.time.temporal.ChronoUnit;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.manfred.incidenttracker.dto.LoginResponse;
import com.manfred.incidenttracker.entity.Role;
import com.manfred.incidenttracker.entity.User;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;



@Service 
public class JwtService {
    
    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-minutes}") long minutes){
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.ttl = Duration.ofMinutes(minutes);
    }

    public LoginResponse issue(User user){
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant exp = now.plus(ttl);

        String idString = String.valueOf(user.getId());

        String token = Jwts.builder()
                        .subject(idString)
                        .claim("role", user.getUserRole().name())
                        .issuedAt(Date.from(now))
                        .expiration(Date.from(exp))
                        .signWith(key)
                        .compact();

        return new LoginResponse(token, exp);

        // Computing now once and deriving exp from it. Two separate Instant.now() calls can make exp - iat come out to 3599 or 3601. Truncating to seconds keeps the expiresAt in the response equal to the exp claim.
    }

    public AuthUser parse(String token){
        
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        Long id = Long.valueOf(claims.getSubject());

        Role role = Role.valueOf(claims.get("role", String.class));

        return new AuthUser(id, role);
    }

}