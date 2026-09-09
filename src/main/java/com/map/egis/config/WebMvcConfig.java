package com.map.egis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 프로젝트 절대 경로 구하기 및 역슬래시(\)를 슬래시(/)로 통일
        String projectRoot = new File("").getAbsolutePath().replace("\\", "/");

        // 2. Windows에서 Spring이 정확히 인식하는 file:/D:/... 규격 생성
        String originalPath = "file:/" + projectRoot + "/storage/original/";
        String compressedPath = "file:/" + projectRoot + "/storage/compressed/";

        System.out.println("==================================================");
        System.out.println("📂 [WebMvcConfig] 최종 매핑 원본 경로: " + originalPath);
        System.out.println("📂 [WebMvcConfig] 최종 매핑 압축 경로: " + compressedPath);
        System.out.println("==================================================");

        registry.addResourceHandler("/images/original/**")
                .addResourceLocations(originalPath);

        registry.addResourceHandler("/images/compressed/**")
                .addResourceLocations(compressedPath);
    }
}