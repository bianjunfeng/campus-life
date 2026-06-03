# 存储实现指南

## 当前实现（本地存储）

### 配置
```yaml
# application-dev.yml
file:
  upload:
    path: uploads  # 相对于项目根目录
    url-prefix: /uploads  # 访问URL前缀
```

### 目录结构
```
campus-life-backend/
└── uploads/
    └── avatars/
        └── 2025/
            └── 12/
                └── 09/
                    └── 997c3170-074b-4898-9ef8-4f8a09ccfc5b.jpg
```

### 访问方式
- 本地访问：`http://localhost:8080/uploads/avatars/2025/12/09/xxx.jpg`
- 需要配置静态资源映射（Spring Boot）

## 迁移到云存储的步骤

### 1. 添加云存储依赖

#### 阿里云 OSS
```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.1</version>
</dependency>
```

#### 腾讯云 COS
```xml
<dependency>
    <groupId>com.qcloud</groupId>
    <artifactId>cos_api</artifactId>
    <version>5.6.89</version>
</dependency>
```

### 2. 创建存储服务接口

```java
public interface FileStorageService {
    String uploadFile(MultipartFile file, String folder);
    void deleteFile(String fileUrl);
    String getFileUrl(String filePath);
}
```

### 3. 实现本地存储（开发环境）

```java
@Service
@Profile("dev")
public class LocalFileStorageService implements FileStorageService {
    @Value("${file.upload.path}")
    private String uploadPath;
    
    @Value("${file.upload.url-prefix}")
    private String urlPrefix;
    
    // 实现上传逻辑
}
```

### 4. 实现云存储（生产环境）

```java
@Service
@Profile("prod")
public class OssFileStorageService implements FileStorageService {
    // 实现OSS上传逻辑
}
```

### 5. 配置切换

```yaml
# application-dev.yml (开发环境)
spring:
  profiles:
    active: dev
file:
  storage:
    type: local  # 本地存储

# application-prod.yml (生产环境)
spring:
  profiles:
    active: prod
file:
  storage:
    type: oss  # 云存储
  oss:
    endpoint: xxx
    access-key-id: xxx
    access-key-secret: xxx
    bucket-name: xxx
```

## 静态资源映射配置

### Spring Boot 配置
```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Value("${file.upload.path}")
    private String uploadPath;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /uploads/** 到本地 uploads 目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
```

## 前端资源部署

### 开发环境
- 使用 Vite 开发服务器
- 静态资源通过开发服务器访问

### 生产环境
- **方案一**：Nginx 托管
  ```
  /var/www/campus-life/
  ├── index.html
  └── assets/
      ├── index-xxx.js
      └── index-xxx.css
  ```

- **方案二**：CDN 托管
  - 将构建后的静态资源上传到 CDN
  - 修改 `index.html` 中的资源路径

### Nginx 配置示例
```nginx
server {
    listen 80;
    server_name yourdomain.com;
    
    # 前端静态资源
    location / {
        root /var/www/campus-life/dist;
        try_files $uri $uri/ /index.html;
    }
    
    # 后端API代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    # 用户上传的文件（如果使用本地存储）
    location /uploads {
        alias /path/to/uploads;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

## 最佳实践总结

### 开发环境
- ✅ 静态资源：Vite 开发服务器
- ✅ 用户资源：本地 `uploads/` 目录

### 生产环境
- ✅ **静态资源**：Nginx 或 CDN
- ✅ **用户资源**：**云对象存储（OSS/COS）**
- ✅ 配置 CDN 加速用户资源访问

### 关键点
1. **开发环境**：简单快速，使用本地存储
2. **生产环境**：**必须使用云存储**，保证高可用
3. **静态资源**：前端打包后通过 Nginx 或 CDN 提供
4. **用户资源**：通过云存储 + CDN 提供，支持高并发访问

