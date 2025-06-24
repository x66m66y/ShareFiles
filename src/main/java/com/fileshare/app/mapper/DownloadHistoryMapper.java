package com.fileshare.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fileshare.app.entity.DownloadHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 下载历史Mapper接口
 */
@Mapper
public interface DownloadHistoryMapper extends BaseMapper<DownloadHistory> {
} 