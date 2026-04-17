package edu.xjtlu.cpt202.backend.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

import edu.xjtlu.cpt202.backend.common.constant.SecurityConstant;

import java.util.Date;
/**
 * JWT - generation and parsing of Tokens
 * 
 * @author DanyiHuang
 * @date 2026/3/29
 */
public class JwtUtils {

    private static String secretKey;
    private static SecretKey KEY;

    public static void initSecret(String key) {
        secretKey = key;
        if (key.getBytes().length < 32) {
            throw new IllegalArgumentException("Secret key must be at least 32 characters long");
        }
        KEY = Keys.hmacShaKeyFor(key.getBytes());
    }

    /**
     * generate JWT Token
     */
    public static String generateToken(Long userId, String role) {
        if (KEY == null) {
            throw new IllegalStateException("JWT secret key not initialized");
        }
        long expirationMillis = SecurityConstant.JWT_EXPIRATION_MILLISECONDS;
        return Jwts.builder()
                .subject("user")
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(KEY)
                .compact();
    }

    /**
     * parsing JWT Token
     */
    public static Claims parseToken(String token) {
        if (KEY == null) {
            throw new IllegalStateException("JWT secret key not initialized");
        }
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * validate Token is valid
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false; 
        } catch (Exception e) {
            return false; 
        }
    }

    public static Long getExpirationTime(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration != null ? expiration.getTime() : null;
        } catch (Exception e) {
            return null;
        }
    }
}