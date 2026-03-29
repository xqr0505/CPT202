package edu.xjtlu.cpt202.backend.common.config;

import edu.xjtlu.cpt202.backend.common.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 秘钥配置类 - 在应用启动时初始化 JWT 秘钥
 * 
 * 秘钥配置方式：
 * 1. application-dev.yml:  jwt.secret: your-dev-secret-key-with-32-characters
 * 2. application-prod.yml: jwt.secret: your-prod-secret-key-with-32-characters
 * 3. application-local.yml: jwt.secret: your-local-secret-key (不上传 Git)
 * 
 * @author DanyiHuang
 * @date 2026/3/29
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.secret:default-secret-key-with-at-least-32-character}")
    private String secret;

    public JwtConfig() {
    }

    /**
     * 初始化后处理 - 在构造函数之后执行
     */
    @javax.annotation.PostConstruct
    public void init() {
        JwtUtils.initSecret(secret);
    }
}
