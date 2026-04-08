package edu.xjtlu.cpt202.backend.common.config;

import edu.xjtlu.cpt202.backend.common.security.JwtAuthenticationFilter;
import edu.xjtlu.cpt202.backend.common.security.RestAccessDeniedHandler;
import edu.xjtlu.cpt202.backend.common.security.RestAuthenticationEntryPoint;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * @author QiranXiao
 * @date 2026/3/27
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserMapper userMapper;

    public SecurityConfig(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Value("${cors.allowed-origins:}")
    private List<String> allowedOrigins;

    @Value("${cors.allowed-origin-patterns:}")
    private List<String> allowedOriginPatterns;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Swagger
                        .requestMatchers(
                                "/doc.html",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // auth
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/auth/logout",
                                "/auth/verify-email",
                                "/auth/reset-password",
                                "/auth/refresh-token"
                        ).permitAll()

                        // FIXME: 暂时允许所有请求直接通过！
                        // .anyRequest().authenticated()
                        .anyRequest().permitAll()
                )


                .addFilterBefore(new JwtAuthenticationFilter(userMapper), UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                        .accessDeniedHandler(new RestAccessDeniedHandler())
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
            if (!allowedOrigins.isEmpty()) {
                configuration.setAllowedOrigins(allowedOrigins);
            } else {
                configuration.setAllowedOrigins(Arrays.asList(
                        "https://your-domain.com"
                        // TODO: replace with production domain
                ));
            }
        } else {
            if (!allowedOriginPatterns.isEmpty()) {
                configuration.setAllowedOriginPatterns(allowedOriginPatterns);
            } else {
                configuration.setAllowedOriginPatterns(Arrays.asList(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://192.168.*.*:*"
                ));
            }
        }

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "X-Auth-Token",
                "X-CSRF-Token",
                "Accept",
                "Cache-Control",
                "Origin"
        ));

        configuration.setExposedHeaders(Arrays.asList(
                "X-Auth-Token",
                "Authorization",
                "Content-Disposition"
        ));

        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
