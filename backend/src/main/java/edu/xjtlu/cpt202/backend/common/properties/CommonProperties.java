package edu.xjtlu.cpt202.backend.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author QiranXiao
 * @date 2026/3/26
 */
@Data
@Component
@ConfigurationProperties(prefix = "expertlink")
public class CommonProperties {

    private Jwt jwt = new Jwt();
    private Oss oss = new Oss();

    @Data
    public static class Jwt {
        private String secretKey;
        private Long expiration;
        private String tokenName;
    }

    @Data
    public static class Oss {
        private String endpoint;
        private String region;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
        private String baseUrl;
    }
}
