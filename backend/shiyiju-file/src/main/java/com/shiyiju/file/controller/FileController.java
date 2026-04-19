package com.shiyiju.file.controller;

import com.shiyiju.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class FileController {

    @Value("${tencent.cos.cdn:https://cdn.shiyiju.com}")
    private String cdnUrl;

    @Value("${tencent.cos.bucket:shiyiju-uploads}")
    private String bucket;

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB for images
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB for videos
    private static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};
    private static final String[] ALLOWED_VIDEO_TYPES = {"video/mp4", "video/quicktime", "video/webm"};

    /**
     * 上传图片 (POST /upload/image)
     * 支持裁剪参数: cropX, cropY, cropWidth, cropHeight
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "cropX", required = false) Integer cropX,
            @RequestParam(value = "cropY", required = false) Integer cropY,
            @RequestParam(value = "cropWidth", required = false) Integer cropWidth,
            @RequestParam(value = "cropHeight", required = false) Integer cropHeight
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("cropX", cropX);
        params.put("cropY", cropY);
        params.put("cropWidth", cropWidth);
        params.put("cropHeight", cropHeight);
        
        return uploadFile(file, "images", ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE, params);
    }

    /**
     * 上传视频 (POST /upload/video)
     */
    @PostMapping("/video")
    public Result<Map<String, String>> uploadVideo(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        return uploadFile(file, "videos", ALLOWED_VIDEO_TYPES, MAX_VIDEO_SIZE, null);
    }

    private Result<Map<String, String>> uploadFile(
            MultipartFile file, 
            String folder, 
            String[] allowedTypes,
            long maxSize,
            Map<String, Object> extraParams
    ) {
        Map<String, String> result = new HashMap<>();
        
        if (file.isEmpty()) {
            return Result.fail(400, "文件不能为空");
        }
        if (file.getSize() > maxSize) {
            return Result.fail(400, "文件大小超出限制");
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
            return Result.fail(400, "不支持的文件类型");
        }

        try {
            // 生成文件路径
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String extension = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            String relativePath = folder + "/" + datePath + "/" + fileName;

            // 本地存储（生产环境应使用 COS/OSS）
            String localPath = "/tmp/shiyiju-uploads/" + relativePath;
            Path path = Paths.get(localPath);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            // 返回 CDN URL
            String fileUrl = cdnUrl + "/" + relativePath;
            result.put("url", fileUrl);
            result.put("filename", fileName);
            result.put("size", String.valueOf(file.getSize()));
            result.put("contentType", contentType);
            
            return Result.success(result);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.fail(500, "文件上传失败: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
