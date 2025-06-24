package com.fileshare.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息实体类
 */
@Data
@TableName("file")
public class FileInfo {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 上传用户ID
     */
    private Long userId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 存储路径
     */
    private String storagePath;

    /**
     * 提取码
     */
    private String extractCode;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 上传时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 状态（0-已删除，1-正常）
     */
    @TableLogic
    private Integer status;
} 