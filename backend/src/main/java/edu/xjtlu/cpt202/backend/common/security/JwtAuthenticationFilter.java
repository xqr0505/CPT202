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

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);

                Claims claims = JwtUtils.parseToken(token);

                Long userId = claims.get("userId", Long.class);
                String role = claims.get("role", String.class);

                if (userId != null && role != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, null);

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    UserContextHolder.setUserId(userId);
                    UserContextHolder.setRole(role);
                }

            } catch (Exception e) {
                logger.debug("JWT parsing failed: " + e.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }
}