package com.fileshare.app.controller;

import com.fileshare.app.config.interceptor.AuthInterceptor;
import com.fileshare.app.controller.dto.WxLoginRequest;
import com.fileshare.app.controller.vo.LoginVo;
import com.fileshare.app.entity.User;
import com.fileshare.app.service.UserService;
import com.fileshare.app.util.JwtUtil;
import com.fileshare.app.util.ResultUtil;
import com.fileshare.app.util.ResultUtil.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Map;

/**
 * 用户控制器
 */
@Api(tags = "用户相关接口")
@RestController
@RequestMapping("/user")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 微信登录
     */
    @ApiOperation("微信登录")
    @PostMapping("/wx-login")
    public Result<LoginVo> wxLogin(@Valid @RequestBody WxLoginRequest loginRequest) {
        // 参数校验
        if (loginRequest == null || !StringUtils.hasText(loginRequest.getCode())) {
            return ResultUtil.validateFailed("参数不完整，需要提供code");
        }
        
        // 记录日志
        logger.info("微信登录请求: code={}, nickName={}, phoneCode={}",
                loginRequest.getCode(), 
                loginRequest.getNickName(),
                loginRequest.getPhoneCode());
        
        try {
            // 调用登录服务
            String avatarUrl = "/static/images/logo.png";
            Result<String> result = userService.wxLogin(
                    loginRequest.getCode(), 
                    loginRequest.getNickName(), 
                    loginRequest.getPhoneCode(), 
                    avatarUrl);
            
            if (result.getCode() != 200) {
                return ResultUtil.error(result.getCode(), result.getMessage());
            }
            
            // 构建登录返回数据
            String token = result.getData();
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            // 获取用户信息
            Result<User> userResult = userService.getUserInfo(userId);
            
            if (userResult.getCode() != 200 || userResult.getData() == null) {
                return ResultUtil.error("获取用户信息失败");
            }
            
            User user = userResult.getData();
            
            // 构建登录返回VO
            LoginVo loginVo = LoginVo.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .avatarUrl(user.getAvatarUrl())
                    .phoneNumber(user.getPhoneNumber())
                    .token(token)
                    .build();
            
            return ResultUtil.success(loginVo);
        } catch (Exception e) {
            logger.error("登录异常", e);
            return ResultUtil.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    @ApiOperation("获取用户信息")
    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        try {
            Long userId = AuthInterceptor.getCurrentUserId(request);
            if (userId == null) {
                return ResultUtil.unauthorized();
            }
            
            return userService.getUserInfo(userId);
        } catch (Exception e) {
            logger.error("获取用户信息异常", e);
            return ResultUtil.error(500, "获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新用户昵称
     */
    @ApiOperation("更新用户昵称")
    @PutMapping("/update-nickname")
    public Result<User> updateUserNickname(@RequestBody Map<String, String> params, HttpServletRequest request) {
        try {
            // 记录请求头和请求体内容
            logger.info("更新昵称请求: Content-Type={}, 请求体={}", 
                request.getContentType(), params);
            
            Long userId = AuthInterceptor.getCurrentUserId(request);
            if (userId == null) {
                return ResultUtil.unauthorized();
            }
            
            String nickname = params.get("nickname");
            if (!StringUtils.hasText(nickname)) {
                return ResultUtil.validateFailed("昵称不能为空");
            }
            
            // 创建用户对象，只设置ID和昵称
            User user = new User();
            user.setId(userId);
            user.setNickname(nickname);
            
            logger.info("更新用户昵称: userId={}, nickname={}", userId, nickname);
            return userService.updateUserInfo(user);
        } catch (Exception e) {
            logger.error("更新用户昵称异常", e);
            return ResultUtil.error(500, "更新用户昵称失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传用户头像
     */
    @ApiOperation("上传用户头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            Long userId = AuthInterceptor.getCurrentUserId(request);
            if (userId == null) {
                return ResultUtil.unauthorized();
            }
            
            if (file.isEmpty()) {
                return ResultUtil.validateFailed("文件不能为空");
            }
            
            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResultUtil.validateFailed("只能上传图片文件");
            }
            
            // 调用服务上传头像
            return userService.uploadAvatar(userId, file);
        } catch (IOException e) {
            logger.error("上传头像异常", e);
            return ResultUtil.error(500, "上传头像失败: " + e.getMessage());
        }
    }
}
