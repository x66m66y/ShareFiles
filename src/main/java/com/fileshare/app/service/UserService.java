package com.fileshare.app.service;

import com.fileshare.app.entity.User;
import com.fileshare.app.util.ResultUtil.Result;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 微信用户登录
     *
     * @param code 微信临时登录凭证
     * @param nickName 用户昵称
     * @param phoneCode 手机号获取凭证
     * @param avatarUrl 用户头像URL (内部默认设置，不需要外部传入)
     * @return 登录结果
     */
    Result<String> wxLogin(String code, String nickName, String phoneCode, String avatarUrl);

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    Result<User> getUserInfo(Long userId);
    
    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 更新结果
     */
    Result<User> updateUserInfo(User user);
    
    /**
     * 上传用户头像
     *
     * @param userId 用户ID
     * @param file 头像文件
     * @return 头像URL
     * @throws IOException IO异常
     */
    Result<String> uploadAvatar(Long userId, MultipartFile file) throws IOException;
} 