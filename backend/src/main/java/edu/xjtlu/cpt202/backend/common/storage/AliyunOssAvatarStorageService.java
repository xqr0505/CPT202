package edu.xjtlu.cpt202.backend.common.storage;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.properties.CommonProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AliyunOssAvatarStorageService implements AvatarStorageService {

    private static final String AVATAR_DIRECTORY = "user-avatars";

    private final CommonProperties commonProperties;

    @Override
    public String uploadUserAvatar(Long userId, MultipartFile file) {
        CommonProperties.Oss ossConfig = commonProperties.getOss();
        ensureConfigured(ossConfig);

        String objectKey = buildObjectKey(userId, file);
        OSS ossClient = null;

        try (InputStream inputStream = file.getInputStream()) {
            ossClient = new OSSClientBuilder().build(
                    buildClientEndpoint(ossConfig),
                    ossConfig.getAccessKeyId().trim(),
                    ossConfig.getAccessKeySecret().trim()
            );

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            if (StringUtils.hasText(file.getContentType())) {
                metadata.setContentType(file.getContentType().trim());
            }
            metadata.setCacheControl("public, max-age=31536000");

            ossClient.putObject(ossConfig.getBucketName().trim(), objectKey, inputStream, metadata);
            return buildPublicUrl(ossConfig, objectKey);
        } catch (IOException exception) {
            throw new BusinessException(
                    ResultCodeEnum.SYSTEM_ERROR.getCode(),
                    "Failed to read avatar file"
            );
        } catch (OSSException | ClientException exception) {
            throw new BusinessException(
                    ResultCodeEnum.SYSTEM_ERROR.getCode(),
                    "Failed to upload avatar to object storage"
            );
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private void ensureConfigured(CommonProperties.Oss ossConfig) {
        if (!StringUtils.hasText(ossConfig.getAccessKeyId())
                || !StringUtils.hasText(ossConfig.getAccessKeySecret())
                || !StringUtils.hasText(ossConfig.getBucketName())) {
            throw new BusinessException(
                    ResultCodeEnum.SYSTEM_ERROR.getCode(),
                    "Avatar storage is not configured. Please set the OSS environment variables first"
            );
        }

        if (!StringUtils.hasText(ossConfig.getEndpoint()) && !StringUtils.hasText(ossConfig.getRegion())) {
            throw new BusinessException(
                    ResultCodeEnum.SYSTEM_ERROR.getCode(),
                    "Avatar storage endpoint is missing. Please configure OSS_ENDPOINT or OSS_REGION"
            );
        }
    }

    private String buildObjectKey(Long userId, MultipartFile file) {
        String extension = resolveExtension(file);
        String dateSegment = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String uniqueSegment = UUID.randomUUID().toString().replace("-", "");
        return String.format(
                "%s/%d/%s-%s.%s",
                AVATAR_DIRECTORY,
                userId,
                dateSegment,
                uniqueSegment,
                extension
        );
    }

    private String resolveExtension(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (StringUtils.hasText(extension)) {
            return extension.trim().toLowerCase(Locale.ROOT);
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/jpg", "image/jpeg" -> "jpg";
            default -> "jpg";
        };
    }

    private String buildClientEndpoint(CommonProperties.Oss ossConfig) {
        if (StringUtils.hasText(ossConfig.getEndpoint())) {
            return normalizeBaseUrl(ossConfig.getEndpoint());
        }

        return String.format("https://oss-%s.aliyuncs.com", ossConfig.getRegion().trim());
    }

    private String buildPublicUrl(CommonProperties.Oss ossConfig, String objectKey) {
        if (StringUtils.hasText(ossConfig.getBaseUrl())) {
            return trimTrailingSlash(ossConfig.getBaseUrl().trim()) + "/" + objectKey;
        }

        if (StringUtils.hasText(ossConfig.getEndpoint())) {
            String endpointWithoutScheme = stripScheme(ossConfig.getEndpoint().trim());
            return "https://" + ossConfig.getBucketName().trim() + "." + endpointWithoutScheme + "/" + objectKey;
        }

        return "https://"
                + ossConfig.getBucketName().trim()
                + ".oss-"
                + ossConfig.getRegion().trim()
                + ".aliyuncs.com/"
                + objectKey;
    }

    private String normalizeBaseUrl(String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return trimTrailingSlash(value);
        }
        return "https://" + trimTrailingSlash(value);
    }

    private String stripScheme(String value) {
        return value
                .replaceFirst("^https://", "")
                .replaceFirst("^http://", "")
                .replaceAll("/+$", "");
    }

    private String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }
}
