package com.shiyiju.product.service;

import com.qiniu.common.QiniuException;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class QiniuUploadService {

    @Value("${qiniu.access-key}")
    private String accessKey;

    @Value("${qiniu.secret-key}")
    private String secretKey;

    @Value("${qiniu.bucket}")
    private String bucket;

    @Value("${qiniu.cdn}")
    private String cdnUrl;

    private UploadManager uploadManager;

    /**
     * 获取上传管理器
     */
    private UploadManager getUploadManager() {
        if (uploadManager == null) {
            // 使用自动区域选择
            Configuration cfg = new Configuration(Region.autoRegion());
            uploadManager = new UploadManager(cfg);
        }
        return uploadManager;
    }

    /**
     * 获取上传凭证
     */
    private String getUploadToken() {
        Auth auth = Auth.create(accessKey, secretKey);
        return auth.uploadToken(bucket);
    }

    /**
     * 上传 MultipartFile 到七牛云
     * @param file 上传的文件
     * @param prefix 存储路径前缀（如 "images", "covers"）
     * @return CDN URL
     */
    public String upload(MultipartFile file, String prefix) throws IOException {
        // 生成存储路径：prefix/yyyy/MM/xxx.jpg
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String key = prefix + "/" + datePath + "/" + fileName;

        // 写入临时文件
        File tempFile = Files.createTempFile("qiniu-upload-", "." + extension).toFile();
        file.transferTo(Objects.requireNonNull(tempFile));

        try {
            return uploadFile(tempFile, key);
        } finally {
            // 删除临时文件
            Files.deleteIfExists(tempFile.toPath());
        }
    }

    /**
     * 上传本地文件到七牛云
     */
    public String upload(File file, String key) throws IOException {
        return uploadFile(file, key);
    }

    private String uploadFile(File file, String key) throws IOException {
        try {
            UploadManager manager = getUploadManager();
            String token = getUploadToken();
            manager.put(file, key, token);
            
            // 返回 CDN URL
            String cdnPath = cdnUrl + "/" + key;
            log.info("文件上传成功: {}", cdnPath);
            return cdnPath;
            
        } catch (QiniuException e) {
            log.error("七牛云上传异常", e);
            throw new IOException("上传失败: " + e.error(), e);
        }
    }

    /**
     * 生成带日期的存储路径
     */
    public String generatePath(String prefix) {
        LocalDate now = LocalDate.now();
        return prefix + "/" + now.getYear() + "/" + String.format("%02d", now.getMonthValue());
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
}