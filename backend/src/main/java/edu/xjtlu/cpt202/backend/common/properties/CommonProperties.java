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
    private Logging logging = new Logging();

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

    @Data
    public static class Logging {
        private boolean enabled = true;
        private int argMaxLength = 120;
        private boolean aiChatProfilingEnabled = true;
        private SlowThreshold slowThreshold = new SlowThreshold();
    }

    @Data
    public static class SlowThreshold {
        private long controllerMs = 800L;
        private long serviceMs = 500L;
    }
}
