package com.campus.campus_life_backend.modules.file.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.modules.file.service.FileStorageService;
import com.campus.campus_life_backend.modules.file.util.FileValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/upload")
@RequirePermission(anyOf = {"file:upload"})
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/avatar")
    public ApiResponse<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Set<String> allowedExtensions = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp"));
        String validationError = FileValidationUtil.validateImageFileComplete(file, allowedExtensions, 5 * 1024 * 1024);
        if (validationError != null) {
            logger.warn("头像上传验证失败: {}", validationError);
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, validationError);
        }

        try {
            String fileUrl = fileStorageService.uploadFile(file, "avatars");
            String originalFilename = file.getOriginalFilename();
            String fileName = originalFilename != null ? originalFilename : "avatar";
            if (fileUrl.contains("/")) {
                fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            }

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", fileName);
            return ApiResponse.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("文件上传参数错误: {}", e.getMessage());
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    @PostMapping("/post-image")
    public ApiResponse<Map<String, String>> uploadPostImage(@RequestParam("file") MultipartFile file) {
        Set<String> allowedExtensions = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp"));
        String validationError = FileValidationUtil.validateImageFileComplete(file, allowedExtensions, 10 * 1024 * 1024);
        if (validationError != null) {
            logger.warn("帖子图片上传验证失败: {}", validationError);
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, validationError);
        }

        try {
            String fileUrl = fileStorageService.uploadFile(file, "posts");
            String originalFilename = file.getOriginalFilename();
            String fileName = originalFilename != null ? originalFilename : "post-image";
            if (fileUrl.contains("/")) {
                fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            }

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", fileName);
            return ApiResponse.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("文件上传参数错误: {}", e.getMessage());
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    @PostMapping("/certificate/student")
    public ApiResponse<Map<String, String>> uploadStudentCertificate(@RequestParam("file") MultipartFile file) {
        Set<String> allowedExtensions = new HashSet<>(Arrays.asList("jpg", "jpeg", "png"));
        String validationError = FileValidationUtil.validateImageFileComplete(file, allowedExtensions, 5 * 1024 * 1024);
        if (validationError != null) {
            logger.warn("学生证上传验证失败: {}", validationError);
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, validationError);
        }

        try {
            String fileUrl = fileStorageService.uploadFile(file, "certificates/student");
            String originalFilename = file.getOriginalFilename();
            String fileName = originalFilename != null ? originalFilename : "certificate";
            if (fileUrl.contains("/")) {
                fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            }

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", fileName);
            return ApiResponse.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("文件上传参数错误: {}", e.getMessage());
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    @PostMapping("/certificate/merchant")
    public ApiResponse<Map<String, String>> uploadMerchantLicense(@RequestParam("file") MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        String validationError;
        if ("pdf".equalsIgnoreCase(extension)) {
            validationError = FileValidationUtil.validatePdfFileComplete(file, 10 * 1024 * 1024);
        } else {
            Set<String> allowedExtensions = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp"));
            validationError = FileValidationUtil.validateImageFileComplete(file, allowedExtensions, 10 * 1024 * 1024);
        }

        if (validationError != null) {
            logger.warn("营业执照上传验证失败: {}", validationError);
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, validationError);
        }

        try {
            String fileUrl = fileStorageService.uploadFile(file, "certificates/merchant");
            String fileName = originalFilename;
            if (fileUrl.contains("/")) {
                fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            }

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", fileName);
            return ApiResponse.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("文件上传参数错误: {}", e.getMessage());
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
}
