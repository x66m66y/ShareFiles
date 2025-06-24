package com.fileshare.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 文件分享应用主类
 */
@SpringBootApplication
@MapperScan("com.fileshare.app.mapper")
@EnableScheduling
public class FileShareApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileShareApplication.class, args);
    }
} 