package com.fileshare.app.util;

import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 提取码生成工具类
 */
@Component
public class ExtractCodeUtil {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 6;
    private static final Random RANDOM = new Random();

    /**
     * 生成随机提取码
     *
     * @return 6位随机提取码
     */
    public String generateExtractCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
} 