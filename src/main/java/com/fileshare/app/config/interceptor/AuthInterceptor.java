package com.fileshare.app.config.interceptor;

import com.fileshare.app.util.JwtUtil;
import com.fileshare.app.util.ResultUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 认证拦截器
 * 用于拦截请求并验证用户是否已登录
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final String USER_ID_ATTRIBUTE = "userId";
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求路径
        String requestURI = request.getRequestURI();
        logger.debug("请求路径: {}", requestURI);
        
        // 从请求头中获取token
        String token = extractToken(request);
        
        // 验证token
        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            logger.warn("未授权访问: {}, token: {}", requestURI, token);
            handleUnauthorized(response);
            return false;
        }
        
        // token有效，获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            logger.warn("无法从token中获取用户ID: {}", token);
            handleUnauthorized(response);
            return false;
        }
        
        // 将用户ID放入请求属性中，方便后续使用
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        return true;
    }
    
    /**
     * 处理未授权的请求
     */
    private void handleUnauthorized(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(ResultUtil.unauthorized()));
        }
    }
    
    /**
     * 从请求头中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    /**
     * 从请求中获取当前登录用户ID
     * 
     * @param request HTTP请求
     * @return 用户ID，如果未登录或无效则返回null
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute(USER_ID_ATTRIBUTE);
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        return null;
    }
} 