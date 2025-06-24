package com.fileshare.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下载历史实体类
 */
@Data
@TableName("download_history")
public class DownloadHistory {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 下载用户ID
     */
    private Long userId;

    /**
     * 下载时间
     */
    private LocalDateTime downloadTime;

    /**
     * 下载IP
     */
    private String downloadIp;
} 