package com.fileshare.app.controller.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * 微信登录请求DTO
 */
@Data
@ToString
@ApiModel("微信登录请求")
public class WxLoginRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @ApiModelProperty("微信登录临时凭证")
    private String code;
    
    @ApiModelProperty("微信昵称")
    private String nickName;
    
    @ApiModelProperty("手机号获取凭证")
    private String phoneCode;
} 