package edu.xjtlu.cpt202.backend.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
/**
 * JWT 工具类 - 负责 Token 的生成和解析
 * 
 * 秘钥配置：
 * - 开发环境：application-dev.yml 中配置
 * - 生产环境：application-prod.yml 中配置（秘钥最少 32 字符）
 * - 本地开发：application-local.yml 中配置（不上传 Git）
 * 
 * 秘钥生成建议：
 * String key = Base64.getEncoder().encodeToString(new byte[32]); // 256-bit key
 * 
 * @author DanyiHuang
 * @date 2026/3/29
 */
public class JwtUtils {

    private static String secretKey;
    private static SecretKey KEY;
    private static final long EXPIRATION = 1000 * 60 * 30; // 30 minutes

    public static void initSecret(String key) {
        secretKey = key;
        if (key.getBytes().length < 32) {
            throw new IllegalArgumentException("Secret key must be at least 32 characters long");
        }
        KEY = Keys.hmacShaKeyFor(key.getBytes());
    }

    /**
     * 生成 JWT Token
     */
    public static String generateToken(Long userId, String role) {
        if (KEY == null) {
            throw new IllegalStateException("JWT secret key not initialized");
        }
        return Jwts.builder()
                .subject("user")
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析 JWT Token
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
     * 验证 Token 是否有效
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