package com.fileshare.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fileshare.app.config.MinioConfig;
import com.fileshare.app.entity.DownloadHistory;
import com.fileshare.app.entity.FileInfo;
import com.fileshare.app.mapper.DownloadHistoryMapper;
import com.fileshare.app.mapper.FileInfoMapper;
import com.fileshare.app.service.FileService;
import com.fileshare.app.util.ExtractCodeUtil;
import com.fileshare.app.util.ResultUtil;
import com.fileshare.app.util.ResultUtil.Result;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件服务实现类
 */
@Service
public class FileServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private ExtractCodeUtil extractCodeUtil;

    @Autowired
    private DownloadHistoryMapper downloadHistoryMapper;

    @Value("${file.expiration-days}")
    private Integer expirationDays;

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param fileName 文件名
     * @param userId 上传用户ID
     * @return 上传结果（含提取码）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<FileInfo> uploadFile(MultipartFile file, String fileName, Long userId) {
        InputStream inputStream = null;
        try {
            // 1. 生成文件存储路径
            String originalFilename = fileName != null && !fileName.trim().isEmpty() ? 
                                     fileName : file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String objectName = generateObjectName(fileExtension);
            
            // 2. 上传到MinIO
            inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            
            // 3. 生成提取码
            String extractCode = extractCodeUtil.generateExtractCode();
            
            // 4. 计算过期时间
            LocalDateTime expireTime = LocalDateTime.now().plusDays(expirationDays);
            
            // 5. 保存文件信息到数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setUserId(userId);
            fileInfo.setFileName(originalFilename);
            fileInfo.setFileSize(file.getSize());
            fileInfo.setFileType(file.getContentType());
            fileInfo.setStoragePath(objectName);
            fileInfo.setExtractCode(extractCode);
            fileInfo.setDownloadCount(0);
            fileInfo.setExpireTime(expireTime);
            fileInfo.setCreateTime(LocalDateTime.now());
            fileInfo.setUpdateTime(LocalDateTime.now());
            fileInfo.setStatus(1);
            save(fileInfo);
            
            return ResultUtil.success(fileInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.error(500, "文件上传失败：" + e.getMessage());
        } finally {
            // 确保关闭输入流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取文件信息（通过提取码）
     *
     * @param extractCode 提取码
     * @return 文件信息
     */
    @Override
    public Result<FileInfo> getFileInfoByCode(String extractCode) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getExtractCode, extractCode);
        wrapper.eq(FileInfo::getStatus, 1);
        
        FileInfo fileInfo = getOne(wrapper);
        
        if (fileInfo == null) {
            return ResultUtil.error(404, "提取码无效或文件已过期");
        }
        
        // 检查文件是否过期
        if (fileInfo.getExpireTime().isBefore(LocalDateTime.now())) {
            return ResultUtil.error(410, "文件已过期");
        }
        
        return ResultUtil.success(fileInfo);
    }

    /**
     * 下载文件
     *
     * @param extractCode 提取码
     * @param userId 下载用户ID
     * @param response HTTP响应
     * @return 下载结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> downloadFile(String extractCode, Long userId, HttpServletResponse response) {
        InputStream inputStream = null;
        try {
            // 1. 获取文件信息
            Result<FileInfo> fileInfoResult = getFileInfoByCode(extractCode);
            if (fileInfoResult.getCode() != 200) {
                return ResultUtil.error(fileInfoResult.getCode(), fileInfoResult.getMessage());
            }
            
            FileInfo fileInfo = fileInfoResult.getData();
            log.info("下载文件: fileId={}, fileName={}, extractCode={}", 
                     fileInfo.getId(), fileInfo.getFileName(), extractCode);
            
            // 2. 从MinIO获取文件
            try {
                inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(fileInfo.getStoragePath())
                                .build()
                );
            } catch (Exception e) {
                log.error("从MinIO获取文件失败: {}", e.getMessage());
                return ResultUtil.error(500, "获取文件失败: " + e.getMessage());
            }
            
            // 3. 设置响应头
            response.setContentType(fileInfo.getFileType());
            response.setContentLengthLong(fileInfo.getFileSize());
            response.setHeader("Content-Disposition", "attachment;filename=" + 
                    URLEncoder.encode(fileInfo.getFileName(), "UTF-8"));
            
            // 4. 写入响应
            try {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    response.getOutputStream().write(buffer, 0, len);
                }
                response.getOutputStream().flush();
            } catch (IOException e) {
                log.error("写入响应流失败: {}", e.getMessage());
                return ResultUtil.error(500, "文件传输失败: " + e.getMessage());
            }
            
            // 5. 更新下载次数
            fileInfo.setDownloadCount(fileInfo.getDownloadCount() + 1);
            updateById(fileInfo);
            
            // 6. 记录下载历史
            if (userId != null) {
                DownloadHistory history = new DownloadHistory();
                history.setFileId(fileInfo.getId());
                history.setUserId(userId);
                history.setDownloadTime(LocalDateTime.now());
                history.setDownloadIp(getIpAddress(response));
                downloadHistoryMapper.insert(history);
                log.info("记录下载历史: fileId={}, userId={}", fileInfo.getId(), userId);
            }
            
            return ResultUtil.success();
        } catch (Exception e) {
            log.error("文件下载异常: {}", e.getMessage(), e);
            return ResultUtil.error(500, "文件下载失败: " + e.getMessage());
        } finally {
            // 确保关闭输入流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    log.error("关闭输入流异常: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 获取用户上传的文件列表
     *
     * @param userId 用户ID
     * @return 文件列表
     */
    @Override
    public Result<List<FileInfo>> getUserFiles(Long userId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId);
        wrapper.eq(FileInfo::getStatus, 1);
        wrapper.orderByDesc(FileInfo::getCreateTime);
        
        List<FileInfo> fileList = list(wrapper);
        return ResultUtil.success(fileList);
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteFile(Long fileId, Long userId) {
        try {
            // 1. 获取文件信息
            FileInfo fileInfo = getById(fileId);
            if (fileInfo == null) {
                return ResultUtil.error(404, "文件不存在");
            }
            
            // 2. 检查权限
            if (!fileInfo.getUserId().equals(userId)) {
                return ResultUtil.error(403, "无权删除该文件");
            }
            
            // 3. 使用MyBatis-Plus的逻辑删除
            boolean result = removeById(fileId);
            if (!result) {
                return ResultUtil.error(500, "删除文件失败");
            }
            
            // 注：不从MinIO中删除实际文件，留待定时任务清理
            
            return ResultUtil.success();
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.error(500, "删除文件失败：" + e.getMessage());
        }
    }

    /**
     * 重置文件提取码
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 新的提取码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> resetExtractCode(Long fileId, Long userId) {
        // 1. 获取文件信息
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            return ResultUtil.error(404, "文件不存在");
        }
        
        // 2. 检查权限
        if (!fileInfo.getUserId().equals(userId)) {
            return ResultUtil.error(403, "无权重置该文件的提取码");
        }
        
        // 3. 生成新的提取码
        String newExtractCode = extractCodeUtil.generateExtractCode();
        
        // 4. 更新文件信息
        fileInfo.setExtractCode(newExtractCode);
        fileInfo.setUpdateTime(LocalDateTime.now());
        updateById(fileInfo);
        
        return ResultUtil.success(newExtractCode);
    }
    
    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * 生成文件存储路径
     *
     * @param extension 文件扩展名
     * @return 存储路径
     */
    private String generateObjectName(String extension) {
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getIpAddress(HttpServletResponse response) {
        // 固定返回值，因为无法从响应对象获取IP地址
        return "0.0.0.0";
    }

    /**
     * 生成文件预签名下载链接
     *
     * @param extractCode 提取码
     * @return 预签名下载链接信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, String>> generatePresignedUrl(String extractCode) {
        try {
            // 1. 获取文件信息
            Result<FileInfo> fileInfoResult = getFileInfoByCode(extractCode);
            if (fileInfoResult.getCode() != 200) {
                return ResultUtil.error(fileInfoResult.getCode(), fileInfoResult.getMessage());
            }
            
            FileInfo fileInfo = fileInfoResult.getData();
            
            // 2. 生成预签名URL，有效期30分钟
            int expiryMinutes = 30;
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucketName())
                            .object(fileInfo.getStoragePath())
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
            
            // 3. 增加下载次数
            fileInfo.setDownloadCount(fileInfo.getDownloadCount() + 1);
            updateById(fileInfo);
            
            // 4. 构建返回信息
            Map<String, String> result = new HashMap<>();
            result.put("downloadUrl", url);
            result.put("fileName", fileInfo.getFileName());
            result.put("fileSize", String.valueOf(fileInfo.getFileSize()));
            result.put("fileType", fileInfo.getFileType());
            result.put("expiryMinutes", String.valueOf(expiryMinutes));
            result.put("expiryInfo", String.format("链接有效期%d分钟，请在%s前使用", 
                    expiryMinutes, 
                    LocalDateTime.now().plusMinutes(expiryMinutes).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            
            return ResultUtil.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.error(500, "生成下载链接失败：" + e.getMessage());
        }
    }
} 