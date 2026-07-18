package com.shiyiju.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置 - 支持 /api/admin/** 路径前缀，静态资源路径映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.local.path:/tmp/shiyiju-uploads}")
    private String uploadLocalPath;

    private static final String[] ADMIN_FRONTEND_ROUTES = {
            "/login",
            "/dashboard",
            "/user/**",
            "/product/**",
            "/order/**",
            "/auction/**",
            "/promotion/**",
            "/community/**",
            "/trade/**",
            "/price-control/**",
            "/resale/**",
            "/system/**"
    };

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
                .addResourceLocations(toFileResourceLocation(uploadLocalPath));
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String route : ADMIN_FRONTEND_ROUTES) {
            registry.addViewController(route).setViewName("forward:/index.html");
        }
    }

    private String toFileResourceLocation(String path) {
        String normalized = path.endsWith("/") ? path : path + "/";
        return "file:" + normalized;
    }
}
