package com.shiyiju.file.service;

import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
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

    private static final Set<String> THUMBNAIL_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif");
    private static final int DEFAULT_THUMB_WIDTH = 960;
    private static final int MAX_THUMB_WIDTH = 1600;

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
        return uploadFile(file, "images", ALLOWED_IMAGE_TYPES, maxImageSize, userId, true);
    }

    /**
     * 上传视频
     */
    public Map<String, String> uploadVideo(MultipartFile file, Long userId) {
        return uploadFile(file, "videos", ALLOWED_VIDEO_TYPES, maxVideoSize, userId, false);
    }

    /**
     * 通用文件上传
     */
    private Map<String, String> uploadFile(MultipartFile file, String folder,
            String[] allowedTypes, long maxSize, Long userId, boolean generateThumbnail) {
        
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
                if (generateThumbnail) {
                    createThumbnailIfNeeded(file, relativePath, contentType);
                }
            }

            // 返回结果
            result.put("url", cdnUrl + "/" + relativePath);
            result.put("thumbUrl", buildThumbnailUrl(relativePath, DEFAULT_THUMB_WIDTH));
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

    public ThumbnailFile resolveThumbnailFile(String fileUrl, Integer width) throws IOException {
        String relativePath = extractRelativePath(fileUrl);
        if (relativePath.isEmpty()) {
            throw new IOException("无法识别图片路径");
        }

        Path originalPath = Paths.get(localPath, relativePath);
        if (!Files.exists(originalPath) || Files.isDirectory(originalPath)) {
            throw new IOException("原图不存在");
        }

        int targetWidth = normalizeThumbnailWidth(width);
        Path thumbnailPath = buildThumbnailPath(relativePath, targetWidth);
        if (!Files.exists(thumbnailPath)) {
            generateThumbnailFile(originalPath, thumbnailPath, detectContentType(originalPath), targetWidth);
        }

        String contentType = Files.probeContentType(thumbnailPath);
        if (contentType == null || contentType.isBlank()) {
            contentType = "image/jpeg";
        }
        return new ThumbnailFile(thumbnailPath, contentType);
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
                deleteThumbnailFiles(relativePath);
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

    private void createThumbnailIfNeeded(MultipartFile file, String relativePath, String contentType) {
        if (!THUMBNAIL_IMAGE_TYPES.contains(String.valueOf(contentType))) {
            return;
        }
        try {
            Path originalPath = Paths.get(localPath, relativePath);
            Path thumbnailPath = buildThumbnailPath(relativePath, DEFAULT_THUMB_WIDTH);
            generateThumbnailFile(originalPath, thumbnailPath, contentType, DEFAULT_THUMB_WIDTH);
        } catch (Exception e) {
            log.warn("生成缩略图失败，将继续使用原图: {}", relativePath, e);
        }
    }

    private void generateThumbnailFile(Path originalPath, Path thumbnailPath, String contentType, int targetWidth) throws IOException {
        if (!THUMBNAIL_IMAGE_TYPES.contains(String.valueOf(contentType))) {
            return;
        }

        BufferedImage originalImage;
        try (InputStream inputStream = Files.newInputStream(originalPath)) {
            originalImage = ImageIO.read(inputStream);
        }
        if (originalImage == null) {
            throw new IOException("图片解码失败");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        if (originalWidth <= 0 || originalHeight <= 0) {
            throw new IOException("图片尺寸无效");
        }

        int safeWidth = Math.min(Math.max(targetWidth, 240), MAX_THUMB_WIDTH);
        if (originalWidth <= safeWidth) {
            Files.createDirectories(thumbnailPath.getParent());
            Files.copy(originalPath, thumbnailPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        int resizedWidth = safeWidth;
        int resizedHeight = Math.max(1, (int) Math.round((double) originalHeight * resizedWidth / originalWidth));
        boolean isPng = "image/png".equalsIgnoreCase(contentType);
        boolean isGif = "image/gif".equalsIgnoreCase(contentType);
        boolean hasAlpha = originalImage.getColorModel().hasAlpha() && (isPng || isGif);
        BufferedImage resizedImage = new BufferedImage(
                resizedWidth,
                resizedHeight,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = resizedImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!hasAlpha) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, resizedWidth, resizedHeight);
            }
            graphics.drawImage(originalImage, 0, 0, resizedWidth, resizedHeight, null);
        } finally {
            graphics.dispose();
        }

        Files.createDirectories(thumbnailPath.getParent());
        String formatName = isGif ? "gif" : (hasAlpha ? "png" : "jpg");
        if (!ImageIO.write(resizedImage, formatName, thumbnailPath.toFile())) {
            throw new IOException("缩略图编码失败");
        }
    }

    private int normalizeThumbnailWidth(Integer width) {
        if (width == null) {
            return DEFAULT_THUMB_WIDTH;
        }
        return Math.min(Math.max(width, 240), MAX_THUMB_WIDTH);
    }

    private String buildThumbnailUrl(String relativePath, int width) {
        return cdnUrl + "/api/file/upload/thumb?url=" + relativePath + "&w=" + normalizeThumbnailWidth(width);
    }

    private Path buildThumbnailPath(String relativePath, int width) {
        String extension = getExtension(relativePath);
        int dotIndex = relativePath.lastIndexOf('.');
        String pathWithoutExt = dotIndex >= 0 ? relativePath.substring(0, dotIndex) : relativePath;
        String outputExtension;
        if ("png".equalsIgnoreCase(extension)) {
            outputExtension = "png";
        } else if ("gif".equalsIgnoreCase(extension)) {
            outputExtension = "gif";
        } else {
            outputExtension = "jpg";
        }
        String thumbRelativePath = pathWithoutExt + "-thumb-" + normalizeThumbnailWidth(width) + "." + outputExtension;
        return Paths.get(localPath, thumbRelativePath);
    }

    private String extractRelativePath(String fileUrl) {
        String value = Objects.toString(fileUrl, "").trim();
        if (value.isEmpty()) {
            return "";
        }
        if (value.startsWith(cdnUrl + "/")) {
            return value.substring((cdnUrl + "/").length());
        }
        if (value.startsWith("/upload/")) {
            return value.substring("/upload/".length());
        }
        if (value.startsWith("upload/")) {
            return value.substring("upload/".length());
        }
        try {
            java.net.URL url = new java.net.URL(value);
            String path = Objects.toString(url.getPath(), "");
            if (path.startsWith("/upload/")) {
                return path.substring("/upload/".length());
            }
        } catch (Exception ignored) {
        }
        return value.contains("/") ? value : "";
    }

    private String detectContentType(Path path) throws IOException {
        String contentType = Files.probeContentType(path);
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        String extension = getExtension(path.getFileName().toString());
        if ("png".equalsIgnoreCase(extension)) {
            return "image/png";
        }
        if ("gif".equalsIgnoreCase(extension)) {
            return "image/gif";
        }
        return "image/jpeg";
    }

    private void deleteThumbnailFiles(String relativePath) throws IOException {
        String pathWithoutExt = relativePath.contains(".")
                ? relativePath.substring(0, relativePath.lastIndexOf('.'))
                : relativePath;
        Path parent = Paths.get(localPath, relativePath).getParent();
        if (parent == null || !Files.exists(parent)) {
            return;
        }
        String prefix = Paths.get(pathWithoutExt).getFileName().toString() + "-thumb-";
        try (var stream = Files.list(parent)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    public static final class ThumbnailFile {
        private final Path path;
        private final String contentType;

        public ThumbnailFile(Path path, String contentType) {
            this.path = path;
            this.contentType = contentType;
        }

        public Path getPath() {
            return path;
        }

        public String getContentType() {
            return contentType;
        }
    }
}
