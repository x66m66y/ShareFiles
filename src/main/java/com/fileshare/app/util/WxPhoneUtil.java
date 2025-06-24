package com.fileshare.app.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 微信手机号获取工具类
 */
@Component
public class WxPhoneUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(WxPhoneUtil.class);

    @Value("${wechat.appid}")
    private String appId;
    
    @Value("${wechat.secret}")
    private String secret;
    
    /**
     * 获取微信手机号
     * @param phoneCode 手机号获取凭证
     * @return 解密后的手机号，如果失败则返回null
     */
    public String getPhoneNumber(String phoneCode) {
        if (phoneCode == null || phoneCode.isEmpty()) {
            logger.warn("手机号获取凭证为空");
            return null;
        }
        
        try {
            // 请求微信API获取用户手机号
            String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + getAccessToken();
            JSONObject param = JSONUtil.createObj();
            param.set("code", phoneCode);
            
            logger.debug("请求微信手机号API: {}", url);
            String result = HttpUtil.post(url, param.toString());
            
            JSONObject response = JSONUtil.parseObj(result);
            if (response.getInt("errcode", -1) == 0) {
                JSONObject phoneInfo = response.getJSONObject("phone_info");
                if (phoneInfo != null) {
                    String phoneNumber = phoneInfo.getStr("phoneNumber");
                    logger.info("成功获取手机号");
                    return phoneNumber;
                }
            } else {
                logger.error("获取手机号失败，错误码：{}, 错误信息：{}", 
                    response.getInt("errcode", -1), response.getStr("errmsg", "未知错误"));
            }
        } catch (Exception e) {
            logger.error("获取手机号异常", e);
        }
        
        return null;
    }
    
    /**
     * 获取微信接口调用凭证（access_token）
     */
    private String getAccessToken() {
        try {
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" 
                + appId + "&secret=" + secret;
            
            logger.debug("请求微信access_token");
            String result = HttpUtil.get(url);
            
            JSONObject response = JSONUtil.parseObj(result);
            String accessToken = response.getStr("access_token");
            if (accessToken != null && !accessToken.isEmpty()) {
                return accessToken;
            } else {
                logger.error("获取access_token失败: {}", response.getStr("errmsg", "未知错误"));
            }
        } catch (Exception e) {
            logger.error("获取access_token异常", e);
        }
        
        return "";
    }
} 