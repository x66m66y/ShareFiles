# 文件分享系统数据库设计

## 用户表 (user)

| 字段名 | 类型 | 说明 |
|-------|------|------|
| id | bigint | 主键 |
| open_id | varchar(128) | 微信OpenID |
| nickname | varchar(64) | 用户昵称 |
| avatar_url | varchar(255) | 头像URL |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |
| status | tinyint | 状态（0-禁用，1-正常） |

## 文件表 (file)

| 字段名 | 类型 | 说明 |
|-------|------|------|
| id | bigint | 主键 |
| user_id | bigint | 上传用户ID |
| file_name | varchar(255) | 原始文件名 |
| file_size | bigint | 文件大小（字节） |
| file_type | varchar(128) | 文件类型 |
| storage_path | varchar(255) | 存储路径 |
| extract_code | varchar(6) | 提取码 |
| download_count | int | 下载次数 |
| expire_time | datetime | 过期时间 |
| create_time | datetime | 上传时间 |
| update_time | datetime | 更新时间 |
| status | tinyint | 状态（0-已删除，1-正常） |

## 下载记录表 (download_history)

| 字段名 | 类型 | 说明 |
|-------|------|------|
| id | bigint | 主键 |
| file_id | bigint | 文件ID |
| user_id | bigint | 下载用户ID |
| download_time | datetime | 下载时间 |
| download_ip | varchar(64) | 下载IP | 