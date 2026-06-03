package com.campus.campus_life_backend.modules.file.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地文件存储服务实现
 * 将文件保存到本地文件系统
 */
@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorageService.class);

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Value("${file.upload.public-base-url:}")
    private String publicBaseUrl;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.address:localhost}")
    private String serverAddress;

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
            
            // 创建上传目录（按日期组织）
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String uploadDir = uploadPath + File.separator + folder + File.separator + dateDir;
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "." + extension;
            String filePath = uploadDir + File.separator + fileName;

            // 保存文件
            Path path = Paths.get(filePath);
            Files.write(path, file.getBytes());

            // 构建文件URL
            // 使用配置的服务器地址和端口，避免硬编码
            String fileUrl = buildPublicFileUrl(folder + "/" + dateDir + "/" + fileName);

            logger.info("文件上传成功: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            // 从URL中提取文件路径
            // URL格式: http://host:port/uploads/folder/date/file
            String pathPart = fileUrl.substring(fileUrl.indexOf(urlPrefix));
            String relativePath = pathPart.substring(urlPrefix.length() + 1); // 去掉开头的 "/"
            String filePath = uploadPath + File.separator + relativePath.replace("/", File.separator);
            
            File file = new File(filePath);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    logger.info("文件删除成功: {}", filePath);
                } else {
                    logger.warn("文件删除失败: {}", filePath);
                }
            } else {
                logger.warn("文件不存在: {}", filePath);
            }
        } catch (Exception e) {
            logger.error("删除文件时发生错误: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getFileUrl(String filePath) {
        // 如果已经是完整URL，直接返回
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            return filePath;
        }
        
        // 构建完整URL
        String relativePath = filePath.replace(uploadPath + File.separator, "").replace(File.separator, "/");
        return buildPublicFileUrl(relativePath);
    }

    private String buildPublicFileUrl(String relativePath) {
        String normalizedRelativePath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String normalizedBaseUrl = publicBaseUrl.endsWith("/")
                    ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                    : publicBaseUrl;
            return normalizedBaseUrl + urlPrefix + "/" + normalizedRelativePath;
        }

        String protocol = "http";
        String host = serverAddress.equals("0.0.0.0") ? "localhost" : serverAddress;
        return String.format("%s://%s:%d%s/%s", protocol, host, serverPort, urlPrefix, normalizedRelativePath);
    }
}


