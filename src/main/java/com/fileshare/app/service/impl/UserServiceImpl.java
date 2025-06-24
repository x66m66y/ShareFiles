package com.fileshare.app.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fileshare.app.entity.User;
import com.fileshare.app.mapper.UserMapper;
import com.fileshare.app.service.UserService;
import com.fileshare.app.util.JwtUtil;
import com.fileshare.app.util.ResultUtil;
import com.fileshare.app.util.ResultUtil.Result;
import com.fileshare.app.util.WxPhoneUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final String DEFAULT_NICKNAME = "微信用户";
    private static final String DEFAULT_AVATAR_URL = "/static/images/logo.png";
    private static final int USER_STATUS_NORMAL = 1;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WxPhoneUtil wxPhoneUtil;

    @Value("${wechat.appid}")
    private String appId;

    @Value("${wechat.secret}")
    private String appSecret;

    @Autowired
    private com.fileshare.app.config.MinioConfig minioConfig;

    @Autowired
    private io.minio.MinioClient minioClient;

    @Value("${minio.imageUrl}")
    private String minioPublicUrl;

    /**
     * 微信用户登录
     *
     * @param code 微信临时登录凭证
     * @param nickName 用户昵称
     * @param phoneCode 手机号获取凭证
     * @param avatarUrl 用户头像URL
     * @return 登录结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> wxLogin(String code, String nickName, String phoneCode, String avatarUrl) {
        try {
            // 获取微信OpenID
            Map<String, Object> wxResult = getWxOpenId(code);
            if (wxResult == null) {
                return ResultUtil.error(400, "微信登录失败: 无法获取OpenID");
            }

            // 获取openid
            String openId = (String) wxResult.get("openid");
            if (!StringUtils.hasText(openId)) {
                logger.warn("获取微信openid失败");
                return ResultUtil.error(400, "获取微信openid失败");
            }

            // 获取手机号
            String phoneNumber = getPhoneNumber(phoneCode);

            // 处理用户昵称和头像
            nickName = StringUtils.hasText(nickName) ? nickName : DEFAULT_NICKNAME;
            avatarUrl = StringUtils.hasText(avatarUrl) ? avatarUrl : DEFAULT_AVATAR_URL;

            // 查询或创建用户
            User user = findOrCreateUser(openId, nickName, avatarUrl, phoneNumber);

            // 生成token
            String token = jwtUtil.generateToken(user.getId());
            logger.info("生成token成功: userId={}", user.getId());

            return ResultUtil.success(token);
        } catch (Exception e) {
            logger.error("登录异常", e);
            return ResultUtil.error(500, "登录失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Override
    public Result<User> getUserInfo(Long userId) {
        try {
            logger.info("获取用户信息, userId: {}", userId);

            User user = getById(userId);

            if (user == null) {
                logger.warn("用户不存在, userId: {}", userId);
                return ResultUtil.error(404, "用户不存在");
            }

            // 处理返回的用户信息
            return ResultUtil.success(sanitizeUserInfo(user));
        } catch (Exception e) {
            logger.error("获取用户信息异常", e);
            return ResultUtil.error(500, "获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<User> updateUserInfo(User user) {
        try {
            logger.info("更新用户昵称: userId={}, nickname={}", user.getId(), user.getNickname());

            // 检查用户是否存在
            User existingUser = getById(user.getId());
            if (existingUser == null) {
                logger.warn("用户不存在, userId: {}", user.getId());
                return ResultUtil.error(404, "用户不存在");
            }

            // 只更新昵称
            boolean needUpdate = false;

            if (StringUtils.hasText(user.getNickname()) && !user.getNickname().equals(existingUser.getNickname())) {
                existingUser.setNickname(user.getNickname());
                needUpdate = true;
                logger.info("更新昵称: {} -> {}", existingUser.getNickname(), user.getNickname());
            }

            if (needUpdate) {
                existingUser.setUpdateTime(LocalDateTime.now());
                updateById(existingUser);
                logger.info("用户昵称更新成功: userId={}", existingUser.getId());
            } else {
                logger.info("用户昵称无变化，不更新: userId={}", existingUser.getId());
            }

            // 返回更新后的用户信息（不包含敏感信息）
            return ResultUtil.success(sanitizeUserInfo(existingUser));
        } catch (Exception e) {
            logger.error("更新用户昵称异常", e);
            return ResultUtil.error(500, "更新用户昵称失败: " + e.getMessage());
        }
    }

    /**
     * 获取微信OpenID
     *
     * @param code 微信临时登录凭证
     * @return 微信接口返回结果
     */
    private Map<String, Object> getWxOpenId(String code) {
        try {
            // 构建微信API URL
            String wxUrl = "https://api.weixin.qq.com/sns/jscode2session" +
                    "?appid=" + appId +
                    "&secret=" + appSecret +
                    "&js_code=" + code +
                    "&grant_type=authorization_code";

            // 使用Hutool的HttpUtil发送GET请求
            String responseBody = HttpUtil.get(wxUrl);

            // 使用Hutool的JSONUtil解析JSON响应
            Map<String, Object> result = JSONUtil.toBean(responseBody, Map.class);

            // 检查微信返回结果
            if (result.containsKey("errcode") && !Objects.equals(0, result.get("errcode"))) {
                String errMsg = result.containsKey("errmsg") ? result.get("errmsg").toString() : "未知错误";
                logger.warn("微信登录失败: {}", errMsg);
                return null;
            }

            return result;
        } catch (Exception e) {
            logger.error("获取微信OpenID异常", e);
            return null;
        }
    }

    /**
     * 获取手机号
     *
     * @param phoneCode 手机号获取凭证
     * @return 手机号
     */
    private String getPhoneNumber(String phoneCode) {
        if (StringUtils.hasText(phoneCode)) {
            try {
                return wxPhoneUtil.getPhoneNumber(phoneCode);
            } catch (Exception e) {
                logger.error("获取手机号异常", e);
            }
        }
        return null;
    }

    /**
     * 查询或创建用户
     *
     * @param openId 微信OpenID
     * @param nickName 用户昵称
     * @param avatarUrl 用户头像URL
     * @param phoneNumber 手机号
     * @return 用户信息
     */
    private User findOrCreateUser(String openId, String nickName, String avatarUrl, String phoneNumber) {
        // 查询用户是否存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getOpenId, openId);
        User user = getOne(wrapper);

        // 用户不存在，则注册
        if (user == null) {
            user = createUser(openId, nickName, avatarUrl, phoneNumber);
        } else {
            // 用户存在，检查是否需要更新信息
            updateUserIfNeeded(user, phoneNumber);
        }

        return user;
    }

    /**
     * 创建新用户
     *
     * @param openId 微信OpenID
     * @param nickName 用户昵称
     * @param avatarUrl 用户头像URL
     * @param phoneNumber 手机号
     * @return 新创建的用户
     */
    private User createUser(String openId, String nickName, String avatarUrl, String phoneNumber) {
        logger.info("创建新用户: nickname={}", nickName);
        User user = new User();
        user.setOpenId(openId);
        user.setNickname(nickName);
        user.setAvatarUrl(avatarUrl);
        user.setPhoneNumber(phoneNumber);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(USER_STATUS_NORMAL);
        save(user);
        logger.info("新用户创建成功: id={}", user.getId());
        return user;
    }

    /**
     * 更新用户信息（如有必要）
     *
     * @param user 现有用户
     * @param phoneNumber 新手机号
     */
    private void updateUserIfNeeded(User user,  String phoneNumber) {
        boolean needUpdate = false;


        // 更新手机号 - 只有当用户提供了新的手机号且与现有的不同时才更新
        if (StringUtils.hasText(phoneNumber) && !phoneNumber.equals(user.getPhoneNumber())) {
            user.setPhoneNumber(phoneNumber);
            needUpdate = true;
        }

        if (needUpdate) {
            user.setUpdateTime(LocalDateTime.now());
            updateById(user);
            logger.info("用户信息已更新: id={}", user.getId());
        }
    }

    /**
     * 清理用户敏感信息，准备返回给前端
     *
     * @param user 用户信息
     * @return 清理后的用户信息
     */
    private User sanitizeUserInfo(User user) {
        // 敏感信息处理
        user.setOpenId(null);  // 不返回openId

        // 检查昵称和头像URL是否存在
        if (user.getNickname() == null || user.getAvatarUrl() == null) {
            logger.warn("用户信息不完整: userId={}", user.getId());

            // 设置默认值
            if (user.getNickname() == null) {
                user.setNickname(DEFAULT_NICKNAME);
            }

            if (user.getAvatarUrl() == null) {
                user.setAvatarUrl(DEFAULT_AVATAR_URL);
            }
        }

        // 处理日期字段，避免序列化问题
        user.setCreateTime(null);
        user.setUpdateTime(null);

        return user;
    }

    /**
     * 上传用户头像
     *
     * @param userId 用户ID
     * @param file 头像文件
     * @return 头像URL
     * @throws IOException IO异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> uploadAvatar(Long userId, MultipartFile file) throws IOException {
        try {
            logger.info("上传用户头像: userId={}, fileName={}, contentType={}",
                userId, file.getOriginalFilename(), file.getContentType());

            // 检查用户是否存在
            User user = getById(userId);
            if (user == null) {
                logger.warn("用户不存在, userId: {}", userId);
                return ResultUtil.error(404, "用户不存在");
            }

            // 生成文件名
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = "avatar/" + userId + "/" + UUID.randomUUID().toString() + fileExtension;

            logger.info("准备上传到MinIO, bucket={}, fileName={}", minioConfig.getBucketName(), fileName);

            try {
                // 上传到MinIO
                minioClient.putObject(
                    io.minio.PutObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
                );

                logger.info("文件上传到MinIO成功");
            } catch (Exception e) {
                logger.error("MinIO上传异常", e);
                return ResultUtil.error(500, "文件存储失败: " + e.getMessage());
            }

            // 构建访问URL - 使用公共可访问的URL
            String avatarUrl = minioPublicUrl + "/" + minioConfig.getBucketName() + "/" + fileName;
            logger.info("生成的头像URL: {}", avatarUrl);

            // 更新用户头像URL
            user.setAvatarUrl(avatarUrl);
            user.setUpdateTime(LocalDateTime.now());
            updateById(user);

            logger.info("用户头像上传成功: userId={}, avatarUrl={}", userId, avatarUrl);

            return ResultUtil.success(avatarUrl);
        } catch (Exception e) {
            logger.error("上传头像异常", e);
            return ResultUtil.error(500, "上传头像失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg"; // 默认扩展名
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
