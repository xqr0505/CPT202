package edu.xjtlu.cpt202.backend.common.security;

import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import edu.xjtlu.cpt202.backend.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器 - 在每个请求前执行
 * 
 * 工作流程：
 * 1. 从请求头获取 Authorization Token
 * 2. 验证并解析 Token
 * 3. 将用户信息放入 SecurityContext 和 UserContextHolder
 * 4. 放行请求到下一个过滤器
 * 
 * @author DanyiHuang
 * @date 2026/3/29
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 从请求头获取 JWT Token
        // 格式: Authorization: Bearer <token>
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                // 2. 提取 Token 字符串（移除 "Bearer " 前缀）
                String token = authHeader.substring(7);

                // 3. 解析 Token 获取用户信息
                Claims claims = JwtUtils.parseToken(token);

                Long userId = claims.get("userId", Long.class);
                String role = claims.get("role", String.class);

                if (userId != null && role != null) {
                    // 4. 将用户信息存储到 Spring Security Context
                    // principal 设置为 userId，后续可通过 SecurityUtils.getCurrentUserId() 获取
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, null);

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // 5. 将用户信息存储到 ThreadLocal Context
                    // 可在同一线程中通过 UserContextHolder 获取
                    UserContextHolder.setUserId(userId);
                    UserContextHolder.setRole(role);
                }

            } catch (Exception e) {
                // Token 无效、过期或解析失败
                // 不设置 authentication，让请求继续
                // 如果后续需要认证的接口会被 SecurityConfig 拦下
                logger.debug("JWT parsing failed: " + e.getMessage());
            }
        }

        // 6. 放行请求到下一个过滤器/Controller
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 请求完成后清除 ThreadLocal，防止线程泄漏
            UserContextHolder.clear();
        }
    }
}