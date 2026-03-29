package edu.xjtlu.cpt202.backend.common.config;

import edu.xjtlu.cpt202.backend.common.security.JwtAuthenticationFilter;
import edu.xjtlu.cpt202.backend.common.security.RestAccessDeniedHandler;
import edu.xjtlu.cpt202.backend.common.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 * 
 * 核心配置：
 * 1. JWT 过滤器 - 在 UsernamePasswordAuthenticationFilter 之前执行
 * 2. 放行接口 - 文档、登录、注册
 * 3. 异常处理 - 401/403 返回 JSON
 * 4. 会话管理 - STATELESS（无状态）
 * 5. CORS - 跨域资源请求
 * 
 * @author QiranXiao
 * @date 2026/3/27
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 安全过滤链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用 CSRF（前后端分离，使用 Token）
                .csrf(csrf -> csrf.disable())

                // 2. 会话管理 - 无状态（JWT 不需要 Session）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. 请求授权配置
                .authorizeHttpRequests(auth -> auth
                        // 3.1 放行 Swagger 文档接口
                        .requestMatchers(
                                "/doc.html",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 3.2 放行认证接口（登录、注册、密码重置等）
                        .requestMatchers(
                                "/auth/login",           // POST - 用户登录
                                "/auth/register",        // POST - 用户注册
                                "/auth/logout",          // POST - 用户登出
                                "/auth/verify-email",    // POST - 发送验证码
                                "/auth/reset-password"   // POST - 密码重置
                        ).permitAll()

                        // 3.3 其他所有请求需要认证
                        .anyRequest().authenticated()
                )

                // 4. 添加 JWT 过滤器（在用户名密码认证过滤器之前）
                .addFilterBefore(new JwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

                // 5. 异常处理 - 返回 JSON 而不是重定向
                .exceptionHandling(exception -> exception
                        // 5.1 未认证 (401)
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                        // 5.2 无权限 (403)
                        .accessDeniedHandler(new RestAccessDeniedHandler())
                )

                // 6. 禁用表单登录和 HTTP Basic（使用 JWT）
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}