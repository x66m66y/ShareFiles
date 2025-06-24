package com.fileshare.app.controller.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("登录信息")
public class LoginVo {

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("用户头像")
    private String avatarUrl;

    @ApiModelProperty("手机号")
    private String phoneNumber;

    @ApiModelProperty("访问令牌")
    private String token;
} 