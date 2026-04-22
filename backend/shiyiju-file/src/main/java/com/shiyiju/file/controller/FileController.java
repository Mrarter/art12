package com.shiyiju.file.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import com.shiyiju.file.entity.FileInfo;
import com.shiyiju.file.mapper.FileInfoMapper;
import com.shiyiju.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileInfoMapper fileInfoMapper;

    @Value("${upload.cdn-url:https://cdn.shiyiju.com}")
    private String cdnUrl;

    @Value("${upload.max-image-size:10485760}")
    private long maxImageSize;

    @Value("${upload.max-video-size:104857600}")
    private long maxVideoSize;

    /**
     * 上传图片 (POST /upload/image)
     * 支持单图上传
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            Map<String, String> result = fileService.uploadImage(file, userId);
            
            // 保存文件信息到数据库（可选）
            saveFileInfo(result, file, userId, "image");
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("图片上传失败", e);
            return Result.fail(500, "图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传视频 (POST /upload/video)
     */
    @PostMapping("/video")
    public Result<Map<String, String>> uploadVideo(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            Map<String, String> result = fileService.uploadVideo(file, userId);
            
            // 保存文件信息到数据库（可选）
            saveFileInfo(result, file, userId, "video");
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("视频上传失败", e);
            return Result.fail(500, "视频上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传图片 (POST /upload/images)
     */
    @PostMapping("/images")
    public Result<List<Map<String, String>>> uploadImages(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("files") MultipartFile[] files
    ) {
        try {
            List<Map<String, String>> results = fileService.batchUploadImages(files, userId);
            return Result.success(results);
        } catch (Exception e) {
            log.error("批量上传失败", e);
            return Result.fail(500, "批量上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件 (DELETE /upload/{id})
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id
    ) {
        FileInfo fileInfo = fileInfoMapper.selectById(id);
        if (fileInfo == null) {
            return Result.fail(404, "文件不存在");
        }
        
        // 验证文件所有权
        if (userId != null && !userId.equals(fileInfo.getUserId())) {
            return Result.fail(403, "无权删除该文件");
        }
        
        // 删除物理文件
        fileService.deleteFile(fileInfo.getFileUrl());
        
        // 删除数据库记录
        fileInfoMapper.deleteById(id);
        
        return Result.success();
    }

    /**
     * 根据URL删除文件 (DELETE /upload/by-url)
     */
    @DeleteMapping("/by-url")
    public Result<Void> deleteFileByUrl(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam String url
    ) {
        FileInfo fileInfo = fileInfoMapper.selectOne(
                new LambdaQueryWrapper<FileInfo>()
                        .eq(FileInfo::getFileUrl, url)
                        .eq(userId != null, FileInfo::getUserId, userId)
        );
        
        if (fileInfo != null) {
            fileService.deleteFile(url);
            fileInfoMapper.deleteById(fileInfo.getId());
        } else {
            // 即使数据库没有记录，也尝试删除物理文件
            fileService.deleteFile(url);
        }
        
        return Result.success();
    }

    /**
     * 获取文件列表 (GET /upload/list)
     */
    @GetMapping("/list")
    public Result<PageResult<FileInfo>> getFileList(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String bizType
    ) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(FileInfo::getUserId, userId);
        }
        if (fileType != null && !fileType.isEmpty()) {
            wrapper.eq(FileInfo::getFileType, fileType);
        }
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(FileInfo::getBizType, bizType);
        }
        
        wrapper.orderByDesc(FileInfo::getCreateTime);
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<FileInfo> pageResult = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<FileInfo> result = 
                fileInfoMapper.selectPage(pageResult, wrapper);
        
        return Result.success(PageResult.of(
                result.getTotal(), 
                page, 
                pageSize, 
                result.getRecords()
        ));
    }

    /**
     * 获取文件详情 (GET /upload/{id})
     */
    @GetMapping("/{id}")
    public Result<FileInfo> getFileInfo(@PathVariable Long id) {
        FileInfo fileInfo = fileInfoMapper.selectById(id);
        if (fileInfo == null) {
            return Result.fail(404, "文件不存在");
        }
        return Result.success(fileInfo);
    }

    /**
     * 保存文件信息到数据库
     */
    private void saveFileInfo(Map<String, String> uploadResult, MultipartFile file, 
            Long userId, String fileType) {
        try {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(uploadResult.get("filename"));
            fileInfo.setOriginalName(uploadResult.get("originalName"));
            fileInfo.setFilePath(uploadResult.get("path"));
            fileInfo.setFileUrl(uploadResult.get("url"));
            fileInfo.setFileSize(Long.parseLong(uploadResult.get("size")));
            fileInfo.setFileType(fileType);
            fileInfo.setMimeType(file.getContentType());
            fileInfo.setExtension(uploadResult.get("filename").substring(
                    uploadResult.get("filename").lastIndexOf(".") + 1));
            fileInfo.setUserId(userId);
            fileInfo.setStatus(1);
            fileInfo.setCreateTime(LocalDateTime.now());
            fileInfo.setUpdateTime(LocalDateTime.now());
            
            fileInfoMapper.insert(fileInfo);
        } catch (Exception e) {
            log.warn("保存文件信息失败: {}", e.getMessage());
            // 保存失败不影响上传流程
        }
    }

    /**
     * 验证文件类型
     */
    @PostMapping("/validate")
    public Result<Map<String, Object>> validateFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        result.put("isImage", fileService.isImage(file));
        result.put("isVideo", fileService.isVideo(file));
        result.put("size", file.getSize());
        result.put("maxImageSize", maxImageSize);
        result.put("maxVideoSize", maxVideoSize);
        result.put("contentType", file.getContentType());
        return Result.success(result);
    }
}
