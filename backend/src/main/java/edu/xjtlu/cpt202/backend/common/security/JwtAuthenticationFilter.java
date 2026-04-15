package edu.xjtlu.cpt202.backend.common.security;

import edu.xjtlu.cpt202.backend.common.enums.AccountStatusEnum;
import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import edu.xjtlu.cpt202.backend.common.utils.JwtUtils;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Date;

/**
 * JWT Authentication Filter - Executes before each request
 * 
 * @author DanyiHuang
 * @date 2026/3/29
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserMapper userMapper;

    public JwtAuthenticationFilter(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

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
                    User user = userMapper.selectById(userId);

                    if (user != null && AccountStatusEnum.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
                        if (isTokenInvalidatedByPasswordChange(claims, user)) {
                            SecurityContextHolder.clearContext();
                        } else {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(userId, null, null);
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            UserContextHolder.setUserId(userId);
                            UserContextHolder.setRole(role);
                        }
                    } else {
                        SecurityContextHolder.clearContext();
                    }
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

    private boolean isTokenInvalidatedByPasswordChange(Claims claims, User user) {
        Date issuedAt = claims.getIssuedAt();

        if (issuedAt == null || user.getPasswordChangedAt() == null) {
            return false;
        }

        return issuedAt.toInstant().isBefore(
                user.getPasswordChangedAt().atZone(ZoneId.systemDefault()).toInstant()
        );
    }
}
