package com.campus.campus_life_backend.modules.file.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件验证工具类
 * 提供文件类型、MIME类型、文件内容等安全验证
 */
public class FileValidationUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(FileValidationUtil.class);
    
    // 允许的图片 MIME 类型
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = new HashSet<>(Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp"
    ));
    
    // 允许的 PDF MIME 类型
    private static final Set<String> ALLOWED_PDF_MIME_TYPES = new HashSet<>(Arrays.asList(
        "application/pdf"
    ));
    
    // 文件头（魔数）定义
    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_HEADER = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF_HEADER_1 = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61}; // GIF87a
    private static final byte[] GIF_HEADER_2 = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61}; // GIF89a
    private static final byte[] WEBP_HEADER = {0x52, 0x49, 0x46, 0x46}; // RIFF
    private static final byte[] PDF_HEADER = {0x25, 0x50, 0x44, 0x46}; // %PDF
    
    /**
     * 验证图片文件（包括扩展名、MIME类型和文件内容）
     * @param file 上传的文件
     * @param allowedExtensions 允许的扩展名集合
     * @return 验证结果，null表示通过，否则返回错误信息
     */
    public static String validateImageFile(MultipartFile file, Set<String> allowedExtensions) {
        if (file == null || file.isEmpty()) {
            return "文件不能为空";
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "文件名不能为空";
        }
        
        // 1. 验证文件名安全性（防止路径遍历攻击）
        String filenameValidation = validateFilename(originalFilename);
        if (filenameValidation != null) {
            return filenameValidation;
        }
        
        // 2. 验证文件扩展名
        String extension = getFileExtension(originalFilename);
        if (extension == null || extension.isEmpty()) {
            return "文件必须包含扩展名";
        }
        
        if (allowedExtensions != null && !allowedExtensions.contains(extension.toLowerCase())) {
            return "不支持的文件格式：" + extension;
        }
        
        // 3. 验证 MIME 类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_MIME_TYPES.contains(contentType.toLowerCase())) {
            logger.warn("文件MIME类型不匹配: 文件名={}, 声明MIME类型={}", originalFilename, contentType);
            return "文件类型不匹配，请上传有效的图片文件";
        }
        
        // 4. 验证文件内容（通过文件头/魔数）
        try {
            String contentValidation = validateImageContent(file);
            if (contentValidation != null) {
                return contentValidation;
            }
        } catch (IOException e) {
            logger.error("验证文件内容时发生错误: {}", e.getMessage(), e);
            return "文件读取失败，请重试";
        }
        
        return null; // 验证通过
    }
    
    /**
     * 验证PDF文件
     */
    public static String validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "文件不能为空";
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "文件名不能为空";
        }
        
        // 验证文件名安全性
        String filenameValidation = validateFilename(originalFilename);
        if (filenameValidation != null) {
            return filenameValidation;
        }
        
        // 验证扩展名
        String extension = getFileExtension(originalFilename);
        if (!"pdf".equalsIgnoreCase(extension)) {
            return "只支持PDF格式";
        }
        
        // 验证 MIME 类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_PDF_MIME_TYPES.contains(contentType.toLowerCase())) {
            logger.warn("PDF文件MIME类型不匹配: 文件名={}, 声明MIME类型={}", originalFilename, contentType);
            return "文件类型不匹配，请上传有效的PDF文件";
        }
        
        // 验证文件内容
        try {
            if (!isPdfFile(file)) {
                return "文件内容不是有效的PDF格式";
            }
        } catch (IOException e) {
            logger.error("验证PDF文件内容时发生错误: {}", e.getMessage(), e);
            return "文件读取失败，请重试";
        }
        
        return null; // 验证通过
    }
    
    /**
     * 验证文件名安全性（防止路径遍历攻击）
     */
    private static String validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "文件名不能为空";
        }
        
        // 检查路径遍历字符
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return "文件名包含非法字符";
        }
        
        // 检查文件名长度
        if (filename.length() > 255) {
            return "文件名过长";
        }
        
        // 检查控制字符
        for (char c : filename.toCharArray()) {
            if (Character.isISOControl(c)) {
                return "文件名包含非法字符";
            }
        }
        
        return null; // 验证通过
    }
    
    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return null;
        }
        
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
    
    /**
     * 验证图片文件内容（通过文件头/魔数）
     */
    private static String validateImageContent(MultipartFile file) throws IOException {
        byte[] header = readFileHeader(file, 12); // 读取前12字节
        
        if (header == null || header.length < 3) {
            return "文件内容无效";
        }
        
        // 检查JPEG
        if (startsWith(header, JPEG_HEADER)) {
            return null; // JPEG文件
        }
        
        // 检查PNG
        if (header.length >= PNG_HEADER.length && startsWith(header, PNG_HEADER)) {
            return null; // PNG文件
        }
        
        // 检查GIF
        if (header.length >= GIF_HEADER_1.length) {
            if (startsWith(header, GIF_HEADER_1) || startsWith(header, GIF_HEADER_2)) {
                return null; // GIF文件
            }
        }
        
        // 检查WebP（需要读取更多字节）
        if (header.length >= WEBP_HEADER.length && startsWith(header, WEBP_HEADER)) {
            // WebP文件头是 RIFF...WEBP，需要进一步验证
            byte[] webpHeader = readFileHeader(file, 12);
            if (webpHeader != null && webpHeader.length >= 8) {
                // 检查是否包含 WEBP 标识
                String headerStr = new String(webpHeader, 0, Math.min(webpHeader.length, 12), StandardCharsets.ISO_8859_1);
                if (headerStr.contains("WEBP")) {
                    return null; // WebP文件
                }
            }
        }
        
        return "文件内容不是有效的图片格式";
    }
    
    /**
     * 检查是否为PDF文件
     */
    private static boolean isPdfFile(MultipartFile file) throws IOException {
        byte[] header = readFileHeader(file, 4);
        return header != null && header.length >= 4 && startsWith(header, PDF_HEADER);
    }
    
    /**
     * 读取文件头
     */
    private static byte[] readFileHeader(MultipartFile file, int length) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[length];
            int bytesRead = inputStream.read(header);
            if (bytesRead < length) {
                // 如果文件太小，返回实际读取的字节
                byte[] actualHeader = new byte[bytesRead];
                System.arraycopy(header, 0, actualHeader, 0, bytesRead);
                return actualHeader;
            }
            return header;
        }
    }
    
    /**
     * 检查字节数组是否以指定前缀开头
     */
    private static boolean startsWith(byte[] array, byte[] prefix) {
        if (array == null || prefix == null || array.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 验证文件大小
     */
    public static String validateFileSize(MultipartFile file, long maxSizeBytes) {
        if (file == null || file.isEmpty()) {
            return "文件不能为空";
        }
        
        long fileSize = file.getSize();
        if (fileSize > maxSizeBytes) {
            long maxSizeMB = maxSizeBytes / (1024 * 1024);
            return String.format("文件大小不能超过%dMB", maxSizeMB);
        }
        
        // 检查文件是否为空
        if (fileSize == 0) {
            return "文件不能为空";
        }
        
        return null; // 验证通过
    }
    
    /**
     * 综合验证图片文件（扩展名、MIME类型、文件内容、文件大小）
     */
    public static String validateImageFileComplete(MultipartFile file, Set<String> allowedExtensions, long maxSizeBytes) {
        // 验证文件大小
        String sizeValidation = validateFileSize(file, maxSizeBytes);
        if (sizeValidation != null) {
            return sizeValidation;
        }
        
        // 验证文件类型和内容
        return validateImageFile(file, allowedExtensions);
    }
    
    /**
     * 综合验证PDF文件
     */
    public static String validatePdfFileComplete(MultipartFile file, long maxSizeBytes) {
        // 验证文件大小
        String sizeValidation = validateFileSize(file, maxSizeBytes);
        if (sizeValidation != null) {
            return sizeValidation;
        }
        
        // 验证文件类型和内容
        return validatePdfFile(file);
    }
}


