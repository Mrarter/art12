package com.shiyiju.file.service;

import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件上传服务
 * 支持本地存储和云存储（COS/OSS）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${upload.storage-type:local}")
    private String storageType;

    @Value("${upload.local.path:/tmp/shiyiju-uploads}")
    private String localPath;

    @Value("${upload.cdn-url:https://cdn.shiyiju.com}")
    private String cdnUrl;

    @Value("${tencent.cos.bucket:shiyiju-uploads}")
    private String cosBucket;

    @Value("${upload.max-image-size:10485760}")
    private long maxImageSize;

    @Value("${upload.max-video-size:104857600}")
    private long maxVideoSize;

    private static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};
    private static final String[] ALLOWED_VIDEO_TYPES = {"video/mp4", "video/quicktime", "video/webm"};

    /**
     * 上传图片
     */
    public Map<String, String> uploadImage(MultipartFile file, Long userId) {
        return uploadFile(file, "images", ALLOWED_IMAGE_TYPES, maxImageSize, userId);
    }

    /**
     * 上传视频
     */
    public Map<String, String> uploadVideo(MultipartFile file, Long userId) {
        return uploadFile(file, "videos", ALLOWED_VIDEO_TYPES, maxVideoSize, userId);
    }

    /**
     * 通用文件上传
     */
    private Map<String, String> uploadFile(MultipartFile file, String folder, 
            String[] allowedTypes, long maxSize, Long userId) {
        
        Map<String, String> result = new HashMap<>();
        
        // 文件校验
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件大小超出限制");
        }

        String contentType = file.getContentType();
        boolean allowed = false;
        for (String type : allowedTypes) {
            if (type.equals(contentType)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的文件类型");
        }

        try {
            // 生成文件路径
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String extension = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            
            // 用户目录隔离
            String userPath = userId != null ? userId.toString() : "anonymous";
            String relativePath = folder + "/" + userPath + "/" + datePath + "/" + fileName;

            // 根据存储类型选择上传方式
            if ("cos".equalsIgnoreCase(storageType)) {
                uploadToCos(file, relativePath);
            } else {
                uploadToLocal(file, relativePath);
            }

            // 返回结果
            result.put("url", cdnUrl + "/" + relativePath);
            result.put("filename", fileName);
            result.put("originalName", file.getOriginalFilename());
            result.put("size", String.valueOf(file.getSize()));
            result.put("contentType", contentType);
            result.put("path", relativePath);
            
            return result;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 本地上传
     */
    private void uploadToLocal(MultipartFile file, String relativePath) throws IOException {
        String fullPath = localPath + "/" + relativePath;
        Path path = Paths.get(fullPath);
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());
        log.info("文件已上传到本地: {}", fullPath);
    }

    /**
     * 腾讯云COS上传
     */
    private void uploadToCos(MultipartFile file, String relativePath) {
        // 这里是COS上传的占位实现
        // 实际项目中需要注入腾讯云COS客户端
        log.info("文件将上传到COS: bucket={}, key={}", cosBucket, relativePath);
        // TODO: 实现COS上传逻辑
        //COSClient client = getCosClient();
        //ObjectMetadata metadata = new ObjectMetadata();
        //metadata.setContentType(file.getContentType());
        //PutObjectRequest request = new PutObjectRequest(cosBucket, relativePath, file.getInputStream(), metadata);
        //client.putObject(request);
    }

    /**
     * 删除文件
     */
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        
        try {
            // 从URL中提取相对路径
            String relativePath = filePath;
            if (filePath.startsWith(cdnUrl)) {
                relativePath = filePath.substring(cdnUrl.length());
                if (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
            }
            
            if ("cos".equalsIgnoreCase(storageType)) {
                deleteFromCos(relativePath);
            } else {
                deleteFromLocal(relativePath);
            }
            
            log.info("文件已删除: {}", filePath);
            
        } catch (Exception e) {
            log.error("文件删除失败: {}", filePath, e);
            // 删除失败不抛异常，避免影响业务
        }
    }

    /**
     * 从本地删除文件
     */
    private void deleteFromLocal(String relativePath) throws IOException {
        String fullPath = localPath + "/" + relativePath;
        Path path = Paths.get(fullPath);
        Files.deleteIfExists(path);
    }

    /**
     * 从COS删除文件
     */
    private void deleteFromCos(String relativePath) {
        log.info("从COS删除文件: {}", relativePath);
        // TODO: 实现COS删除逻辑
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 批量上传图片
     */
    public List<Map<String, String>> batchUploadImages(MultipartFile[] files, Long userId) {
        List<Map<String, String>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    results.add(uploadImage(file, userId));
                } catch (Exception e) {
                    log.error("批量上传文件失败: {}", file.getOriginalFilename(), e);
                    // 继续处理其他文件
                }
            }
        }
        return results;
    }

    /**
     * 验证文件是否为图片
     */
    public boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        for (String type : ALLOWED_IMAGE_TYPES) {
            if (type.equals(contentType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证文件是否为视频
     */
    public boolean isVideo(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        for (String type : ALLOWED_VIDEO_TYPES) {
            if (type.equals(contentType)) {
                return true;
            }
        }
        return false;
    }
}
