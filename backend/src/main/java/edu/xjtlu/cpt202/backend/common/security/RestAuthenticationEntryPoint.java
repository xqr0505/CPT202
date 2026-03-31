package edu.xjtlu.cpt202.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.common.result.ResultCodeEnum;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证请求入口 - 返回 401 Unauthorized
 * 当用户未登录或 Token 无效时触发
 * @author DanyiHuang
 * @date 2026/3/29
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<?> result = Result.fail(ResultCodeEnum.UNAUTHORIZED.getCode(), "Unauthorized: " + authException.getMessage());
        
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(result));
    }
}
