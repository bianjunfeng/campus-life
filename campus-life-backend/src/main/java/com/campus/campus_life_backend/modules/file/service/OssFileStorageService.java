package com.campus.campus_life_backend.modules.file.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 文件存储服务实现
 * 将文件上传到阿里云对象存储
 */
@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "oss")
public class OssFileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(OssFileStorageService.class);

    private final OSS ossClient;

    @Value("${file.oss.endpoint}")
    private String endpoint;

    @Value("${file.oss.bucket-name}")
    private String bucketName;

    @Value("${file.oss.url-prefix:}")
    private String urlPrefix;

    public OssFileStorageService(OSS ossClient) {
        this.ossClient = ossClient;
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // 验证文件
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("文件不能为空");
            }

            // 获取文件扩展名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new IllegalArgumentException("文件名不能为空");
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

            // 构建 OSS 对象键（文件路径）
            // 格式: folder/yyyy/MM/dd/uuid.extension
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = UUID.randomUUID().toString() + "." + extension;
            String objectKey = folder + "/" + dateDir + "/" + fileName;

            // 上传文件
            try (InputStream inputStream = file.getInputStream()) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectKey, inputStream);
                ossClient.putObject(putObjectRequest);
            }

            // 构建文件URL
            // 如果配置了 url-prefix，使用配置的值；否则使用默认的 OSS 域名
            String fileUrl;
            if (urlPrefix != null && !urlPrefix.isEmpty()) {
                // 使用自定义域名或 CDN 域名
                fileUrl = urlPrefix + "/" + objectKey;
                // 确保 URL 以 http:// 或 https:// 开头
                if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
                    fileUrl = "https://" + fileUrl;
                }
            } else {
                // 使用默认 OSS 域名
                fileUrl = String.format("https://%s.%s/%s", bucketName, endpoint, objectKey);
            }

            logger.info("文件上传到OSS成功: {}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            logger.error("文件上传到OSS失败: {}", e.getMessage(), e);
            throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            // 从URL中提取对象键
            // URL格式可能是:
            // 1. https://bucket.endpoint/folder/date/file
            // 2. https://custom-domain/folder/date/file
            String objectKey;
            if (fileUrl.contains(bucketName + "." + endpoint)) {
                // 使用默认 OSS 域名
                int index = fileUrl.indexOf(bucketName + "." + endpoint);
                objectKey = fileUrl.substring(index + bucketName.length() + endpoint.length() + 1);
            } else if (fileUrl.contains(urlPrefix)) {
                // 使用自定义域名
                int index = fileUrl.indexOf(urlPrefix);
                objectKey = fileUrl.substring(index + urlPrefix.length() + 1);
            } else {
                // 尝试从URL中提取路径部分
                int lastSlash = fileUrl.lastIndexOf("/");
                int secondLastSlash = fileUrl.lastIndexOf("/", lastSlash - 1);
                if (secondLastSlash > 0) {
                    objectKey = fileUrl.substring(secondLastSlash + 1);
                } else {
                    logger.warn("无法从URL中提取对象键: {}", fileUrl);
                    return;
                }
            }

            ossClient.deleteObject(bucketName, objectKey);
            logger.info("OSS文件删除成功: {}", objectKey);
        } catch (Exception e) {
            logger.error("删除OSS文件时发生错误: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getFileUrl(String filePath) {
        // 如果已经是完整URL，直接返回
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            return filePath;
        }

        // 构建 OSS URL
        if (urlPrefix != null && !urlPrefix.isEmpty()) {
            String url = urlPrefix + "/" + filePath;
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            return url;
        } else {
            return String.format("https://%s.%s/%s", bucketName, endpoint, filePath);
        }
    }
}


