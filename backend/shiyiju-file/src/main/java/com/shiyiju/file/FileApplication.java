package com.shiyiju.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableDiscoveryClient
@SpringBootApplication(
    scanBasePackages = {"com.shiyiju.file", "com.shiyiju.common"}
)
public class FileApplication implements WebMvcConfigurer {
    
    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /upload/** 到本地文件目录
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:/Users/master/CodeBuddy/art12/uploads/");
    }
}
