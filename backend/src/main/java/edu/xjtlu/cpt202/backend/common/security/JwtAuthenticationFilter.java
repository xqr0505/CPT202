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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
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
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
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
                String tokenRole = claims.get("role", String.class);

                if (userId != null) {
                    User user = userMapper.selectById(userId);

                    if (user != null && AccountStatusEnum.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
                        if (isTokenInvalidatedByPasswordChange(claims, user)) {
                            SecurityContextHolder.clearContext();
                        } else {
                            String dbRole = normalizeRole(user.getRole());
                            String normalizedTokenRole = normalizeRole(tokenRole);
                            List<GrantedAuthority> authorities = buildAuthorities(dbRole);

                            if (dbRole == null
                                    || normalizedTokenRole == null
                                    || !dbRole.equals(normalizedTokenRole)
                                    || authorities.isEmpty()) {
                                SecurityContextHolder.clearContext();
                            } else {
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                UserContextHolder.setUserId(userId);
                                UserContextHolder.setRole(dbRole);
                            }
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

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        return normalizedRole.isEmpty() ? null : normalizedRole;
    }

    private List<GrantedAuthority> buildAuthorities(String role) {
        if (role == null) {
            return List.of();
        }
        return switch (role) {
            case "ADMIN", "SPECIALIST", "CUSTOMER" -> List.of(new SimpleGrantedAuthority("ROLE_" + role));
            default -> List.of();
        };
    }
}
