package edu.xjtlu.cpt202.backend.common.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.properties.CommonProperties;
import edu.xjtlu.cpt202.backend.common.service.FileUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final CommonProperties commonProperties;

    public FileUploadServiceImpl(CommonProperties commonProperties) {
        this.commonProperties = commonProperties;
    }

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Image file is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Image size must not exceed 2MB");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Only JPG and PNG images are supported");
        }

        CommonProperties.Oss oss = commonProperties.getOss();
        if (isBlank(oss.getEndpoint()) || isBlank(oss.getAccessKeyId()) || isBlank(oss.getAccessKeySecret()) || isBlank(oss.getBucketName())) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "OSS configuration is incomplete");
        }

        String normalizedFolder = (folder == null || folder.isBlank()) ? "uploads" : folder.trim();
        String datePath = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String objectKey = normalizedFolder + "/" + datePath + "/" + UUID.randomUUID() + "." + extension;

        OSS client = null;
        try (InputStream inputStream = file.getInputStream()) {
            client = new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret());
            client.putObject(oss.getBucketName(), objectKey, inputStream);
            return buildPublicUrl(oss.getEndpoint(), oss.getBucketName(), objectKey);
        } catch (IOException ex) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Failed to read image file");
        } catch (Exception ex) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Failed to upload image");
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Image file extension is required");
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String buildPublicUrl(String endpoint, String bucketName, String objectKey) {
        String normalizedEndpoint = endpoint.trim();
        if (normalizedEndpoint.startsWith("http://") || normalizedEndpoint.startsWith("https://")) {
            String protocol = normalizedEndpoint.startsWith("https://") ? "https://" : "http://";
            String host = normalizedEndpoint.replaceFirst("^https?://", "");
            return protocol + bucketName + "." + host + "/" + objectKey;
        }
        return "https://" + bucketName + "." + normalizedEndpoint + "/" + objectKey;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
