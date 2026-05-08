package com.shiyiju.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置 - 支持 /api/admin/** 路径前缀，静态资源路径映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 启用路径匹配器的后缀模式
        configurer.setUseSuffixPatternMatch(false);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 默认头像静态资源映射：/images/** → classpath:/static/images/
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");

        // 上传文件映射：/upload/** → 本地 uploads 目录
        // 前端 getFullImageUrl 会将 /upload/ 路径的图片URL转换为相对路径，
        // 需要在此处代理到文件服务或本地目录
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:/Users/master/CodeBuddy/art12/uploads/");
    }
}
