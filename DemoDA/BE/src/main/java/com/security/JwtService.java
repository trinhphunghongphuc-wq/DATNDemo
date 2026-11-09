package com.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import java.util.Base64;

@Service
public class JwtService {

    // ít nhất 32 ký tự
//    private static final String SECRET_KEY = "mysecretkeymysecretkeymysecretkey12";
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 1 ngày

//    private SecretKey getSignInKey() {
//        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
//    }

    private final SecretKey signingKey;


    public JwtService(
            @Value("${security.jwt.secret-base64}") String secretBase64
    ) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT_SECRET_BASE64 must not be blank"
            );
        }

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(secretBase64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "JWT_SECRET_BASE64 must be valid Base64",
                    exception
            );
        }

        // Khóa 256 bit cho HS256.
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET_BASE64 must decode to exactly 32 bytes"
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey getSignInKey() {
        return signingKey;
    }

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}