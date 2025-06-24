package com.fileshare.app.service;

import com.fileshare.app.entity.FileInfo;
import com.fileshare.app.util.ResultUtil.Result;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param fileName 文件名
     * @param userId 上传用户ID
     * @return 上传结果（含提取码）
     */
    Result<FileInfo> uploadFile(MultipartFile file, String fileName, Long userId);

    /**
     * 获取文件信息（通过提取码）
     *
     * @param extractCode 提取码
     * @return 文件信息
     */
    Result<FileInfo> getFileInfoByCode(String extractCode);

    /**
     * 下载文件
     *
     * @param extractCode 提取码
     * @param userId 下载用户ID
     * @param response HTTP响应
     * @return 下载结果
     */
    Result<Void> downloadFile(String extractCode, Long userId, HttpServletResponse response);

    /**
     * 生成文件预签名下载链接
     *
     * @param extractCode 提取码
     * @return 预签名下载链接信息
     */
    Result<Map<String, String>> generatePresignedUrl(String extractCode);

    /**
     * 获取用户上传的文件列表
     *
     * @param userId 用户ID
     * @return 文件列表
     */
    Result<List<FileInfo>> getUserFiles(Long userId);

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 删除结果
     */
    Result<Void> deleteFile(Long fileId, Long userId);

    /**
     * 重置文件提取码
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 新的提取码
     */
    Result<String> resetExtractCode(Long fileId, Long userId);
} 