package com.fileshare.app.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 * 使用JJWT库实现标准JWT的生成和验证
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 从token中解析用户ID
     *
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            String subject = getClaimFromToken(token, Claims::getSubject);
            logger.debug("从token获取subject: {}", subject);
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            logger.error("从token获取用户ID失败: subject不是有效的用户ID", e);
            return null;
        } catch (Exception e) {
            logger.error("从token获取用户ID失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取token的过期时间
     *
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从token中获取指定的声明信息
     *
     * @param token JWT令牌
     * @param claimsResolver 声明解析函数
     * @return 声明值
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从token中获取所有声明信息
     *
     * @param token JWT令牌
     * @return 所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    /**
     * 检查token是否已过期
     *
     * @param token JWT令牌
     * @return 是否已过期
     */
    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            logger.error("检查token过期失败", e);
            return true;
        }
    }

    /**
     * 生成token
     *
     * @param userId 用户ID
     * @return JWT令牌
     */
    public String generateToken(Long userId) {
        try {
            Map<String, Object> claims = new HashMap<>();
            return doGenerateToken(claims, String.valueOf(userId));
        } catch (Exception e) {
            logger.error("生成token异常", e);
            throw new RuntimeException("Token生成失败", e);
        }
    }

    /**
     * 生成JWT令牌
     *
     * @param claims 额外声明
     * @param subject 主题(用户ID)
     * @return JWT令牌
     */
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /**
     * 验证token是否有效
     *
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                logger.warn("token为空");
                return false;
            }
            
            // 验证token是否过期
            boolean isExpired = isTokenExpired(token);
            if (isExpired) {
                logger.info("token已过期");
                return false;
            }
            
            logger.debug("token验证通过");
            return true;
        } catch (io.jsonwebtoken.SignatureException e) {
            logger.error("验证token异常: 签名无效", e);
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.info("token已过期");
            return false;
        } catch (Exception e) {
            logger.error("验证token异常: {}", e.getMessage(), e);
            return false;
        }
    }
} 