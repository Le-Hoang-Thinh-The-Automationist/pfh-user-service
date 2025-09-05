package com.pfh.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.pfh.user.exception.JsonFormatInvalidException;
import com.pfh.user.service.UserService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtUtil {
    private final String SECRET = "your-secret-key"; // Replace with a secure secret key
    private final long EXPIRATION_TIME = 900_000; // 15 minutes

    private final UserService userService;

    public String generateToken(String subject, Map<String, Object> claims ) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }


    public Long extractId(String token) {
        long id;
        try {
            id = Long.parseLong(extractClaim(token, Claims::getSubject));
            // use id safely
        } catch (NumberFormatException e) {
            throw new JsonFormatInvalidException("The request API json format is incorrect");
        }
        return id;
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token).getBody();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token) {
        boolean credentialValid = false;
        
        // // Check if the web token user
        // try {
        //     credentialValid = userService.getUserById(extractId(token))
        //                         // After get the user by ID, check if the email is still
        //                         // the same as in the token or has it changed
        //                         .getEmail().equals(token);

        // } catch (EntityNotFoundException ex) {}

        return (credentialValid && !isTokenExpired(token));
    }
}