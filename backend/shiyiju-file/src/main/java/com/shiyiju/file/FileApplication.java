package com.shiyiju.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableDiscoveryClient
@SpringBootApplication(
    scanBasePackages = {"com.shiyiju.file", "com.shiyiju.common"}
)
public class FileApplication implements WebMvcConfigurer {

    @Value("${upload.local.path:/tmp/shiyiju-uploads}")
    private String uploadLocalPath;
    
    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /upload/** 到本地文件目录
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(toFileResourceLocation(uploadLocalPath));
    }

    private String toFileResourceLocation(String path) {
        String normalized = path.endsWith("/") ? path : path + "/";
        return "file:" + normalized;
    }
}
