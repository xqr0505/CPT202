package edu.xjtlu.cpt202.backend.common.config;

import edu.xjtlu.cpt202.backend.common.utils.JwtUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author DanyiHuang
 * @date 2026/3/29
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.secret:default-secret-key-with-at-least-32-character}")
    private String secret;

    public JwtConfig() {
    }

    @PostConstruct
    public void init() {
        JwtUtils.initSecret(secret);
    }
}
