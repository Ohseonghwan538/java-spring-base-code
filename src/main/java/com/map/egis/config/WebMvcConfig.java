package com.map.egis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 프로젝트 루트 기준 D:/03_project/.../storage 디렉터리 동적 매핑
        String projectRoot = new File("").getAbsolutePath();
        String uploadPath = "file:///" + projectRoot.replace("\\", "/") + "/storage/";

        registry.addResourceHandler("/images/**")
                .addResourceLocations(uploadPath);
    }
}