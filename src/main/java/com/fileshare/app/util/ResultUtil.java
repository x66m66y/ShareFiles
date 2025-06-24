package com.fileshare.app.util;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结果工具类
 */
public class ResultUtil {

    /**
     * 成功返回
     *
     * @param data 数据
     * @return 统一返回结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功返回
     *
     * @return 统一返回结果
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 成功返回结果
     * @param data 返回的数据
     * @param message 提示信息
     */
    public static <T> Result<T> success(T data, String message) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败返回
     *
     * @param code 错误码
     * @param message 错误信息
     * @return 统一返回结果
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败返回结果
     * @param message 提示信息
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 参数错误返回
     *
     * @param message 错误信息
     * @return 统一返回结果
     */
    public static <T> Result<T> paramError(String message) {
        return new Result<>(400, message, null);
    }

    /**
     * 参数验证失败返回结果
     */
    public static <T> Result<T> validateFailed() {
        return error(400, "参数验证失败");
    }

    /**
     * 参数验证失败返回结果
     * @param message 提示信息
     */
    public static <T> Result<T> validateFailed(String message) {
        return error(400, message);
    }

    /**
     * 未授权返回
     *
     * @return 统一返回结果
     */
    public static <T> Result<T> unauthorized() {
        return new Result<>(401, "暂未登录或token已过期", null);
    }

    /**
     * 未授权返回结果
     */
    public static <T> Result<T> forbidden() {
        return error(403, "没有相关权限");
    }

    /**
     * 服务器错误返回
     *
     * @return 统一返回结果
     */
    public static <T> Result<T> serverError() {
        return new Result<>(500, "服务器错误", null);
    }

    /**
     * 统一返回结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel("响应结果")
    public static class Result<T> {
        /**
         * 状态码
         */
        @ApiModelProperty("状态码")
        private int code;

        /**
         * 消息
         */
        @ApiModelProperty("消息")
        private String message;

        /**
         * 数据
         */
        @ApiModelProperty("数据")
        private T data;
    }
} 