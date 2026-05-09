package edu.xjtlu.cpt202.backend.common.config;

import edu.xjtlu.cpt202.backend.common.security.JwtAuthenticationFilter;
import edu.xjtlu.cpt202.backend.common.security.RestAccessDeniedHandler;
import edu.xjtlu.cpt202.backend.common.security.RestAuthenticationEntryPoint;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@EnableMethodSecurity
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

                        .requestMatchers(
                                "/swagger-ui.html",        
                                "/api/auth/refresh-token",
                                "/auth/refresh-token",
                                "/api/auth/login",
                                "/auth/login",
                                "/api/auth/register",
                                "/auth/register",
                                "/api/auth/verify-email",
                                "/auth/verify-email",
                                "/api/auth/logout",
                                "/auth/logout",           
                                "/api/auth/reset-password/**",
                                "/auth/reset-password/**",
                                "/api/v1/categories",
                                "/api/v1/specialists", 
                                "/doc.html",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/error/**"
                        ).permitAll()

                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/specialist/**").hasRole("SPECIALIST")
                        .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/user/**").hasAnyRole("ADMIN", "SPECIALIST", "CUSTOMER")
                        .requestMatchers("/api/v1/ai/**").hasRole("CUSTOMER")
                        .requestMatchers(
                                "/api/v1/categories",
                                "/api/v1/specialists",
                                "/api/v1/specialists/**",
                                "/api/v1/booking-topics",
                                "/api/v1/booking-topics/**"
                        ).authenticated()

                        .anyRequest().authenticated()
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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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

        configuration.addAllowedOrigin("http://120.26.245.169");
        configuration.addAllowedOrigin("https://120.26.245.169");

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
