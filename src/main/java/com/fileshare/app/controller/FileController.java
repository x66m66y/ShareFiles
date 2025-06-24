package com.fileshare.app.controller;

import com.fileshare.app.config.interceptor.AuthInterceptor;
import com.fileshare.app.entity.FileInfo;
import com.fileshare.app.service.FileService;
import com.fileshare.app.util.ResultUtil;
import com.fileshare.app.util.ResultUtil.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/file")
public class FileController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;
    
    @Value("${server.port}")
    private String serverPort;
    
    @Value("${file.download.base-url:}")
    private String fileDownloadBaseUrl;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<FileInfo> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileName", required = false) String fileName,
            HttpServletRequest request) {
        Long userId = AuthInterceptor.getCurrentUserId(request);
        if (userId == null) {
            return ResultUtil.unauthorized();
        }
        return fileService.uploadFile(file, fileName, userId);
    }

    /**
     * 根据提取码获取文件信息
     */
    @GetMapping("/info/{extractCode}")
    public Result<FileInfo> getFileInfo(@PathVariable String extractCode) {
        return fileService.getFileInfoByCode(extractCode);
    }
    
    /**
     * 生成文件下载链接
     */
    @GetMapping("/download-url/{extractCode}")
    public Result<Map<String, String>> generateDownloadUrl(@PathVariable String extractCode) {
        return fileService.generatePresignedUrl(extractCode);
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{extractCode}")
    public Result<Void> downloadFile(
            @PathVariable String extractCode,
            HttpServletRequest request,
            HttpServletResponse response) {
        // 这里允许未登录用户下载，但如果登录了会记录下载历史
        Long userId = AuthInterceptor.getCurrentUserId(request);
        return fileService.downloadFile(extractCode, userId, response);
    }

    /**
     * 获取用户上传的文件列表
     */
    @GetMapping("/list")
    public Result<List<FileInfo>> getUserFiles(HttpServletRequest request) {
        Long userId = AuthInterceptor.getCurrentUserId(request);
        if (userId == null) {
            return ResultUtil.unauthorized();
        }
        return fileService.getUserFiles(userId);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    public Result<Void> deleteFile(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        Long userId = AuthInterceptor.getCurrentUserId(request);
        if (userId == null) {
            return ResultUtil.unauthorized();
        }
        return fileService.deleteFile(fileId, userId);
    }

    /**
     * 重置文件提取码
     */
    @PutMapping("/reset-code/{fileId}")
    public Result<String> resetExtractCode(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        Long userId = AuthInterceptor.getCurrentUserId(request);
        if (userId == null) {
            return ResultUtil.unauthorized();
        }
        return fileService.resetExtractCode(fileId, userId);
    }
} 