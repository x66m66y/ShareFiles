package com.fileshare.app.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fileshare.app.config.MinioConfig;
import com.fileshare.app.entity.FileInfo;
import com.fileshare.app.service.impl.FileServiceImpl;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件清理定时任务
 */
@Slf4j
@Component
public class FileCleanTask {

    @Autowired
    private FileServiceImpl fileService;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 每天凌晨2点执行清理过期文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredFiles() {
        log.info("开始清理过期文件...");
        
        try {
            // 1. 查询所有过期但未删除的文件
            LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.lt(FileInfo::getExpireTime, LocalDateTime.now());
            wrapper.eq(FileInfo::getStatus, 1);
            
            List<FileInfo> expiredFiles = fileService.list(wrapper);
            
            log.info("发现 {} 个过期文件", expiredFiles.size());
            
            // 2. 逐个删除文件
            for (FileInfo fileInfo : expiredFiles) {
                try {
                    // 2.1 从MinIO中删除文件
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(minioConfig.getBucketName())
                                    .object(fileInfo.getStoragePath())
                                    .build()
                    );
                    
                    // 2.2 更新数据库状态（逻辑删除）
                    fileService.removeById(fileInfo.getId());
                    
                    log.info("已清理文件：{}", fileInfo.getFileName());
                } catch (Exception e) {
                    log.error("清理文件 {} 失败: {}", fileInfo.getFileName(), e.getMessage());
                }
            }
            
            log.info("过期文件清理完成");
        } catch (Exception e) {
            log.error("文件清理任务执行异常: {}", e.getMessage());
        }
    }
} 