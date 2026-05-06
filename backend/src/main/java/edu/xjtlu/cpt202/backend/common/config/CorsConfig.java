package edu.xjtlu.cpt202.backend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author QiranXiao
 * @date 2026/3/26
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // allow development host(s) and the production frontend origin
                .allowedOrigins(
                    "http://localhost:5173",
                    "http://127.0.0.1:5173",
                    "http://120.26.245.169",
                    "https://120.26.245.169"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);

    }
}
