package com.campus.campus_life_backend.modules.file.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 * 提供统一的文件上传、删除、获取URL方法
 * 支持本地存储和云对象存储两种实现
 */
public interface FileStorageService {
    
    /**
     * 上传文件
     * @param file 文件对象
     * @param folder 文件夹路径（如 "avatars"）
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String folder);
    
    /**
     * 删除文件
     * @param fileUrl 文件URL
     */
    void deleteFile(String fileUrl);
    
    /**
     * 获取文件访问URL
     * @param filePath 文件路径
     * @return 文件访问URL
     */
    String getFileUrl(String filePath);
}


