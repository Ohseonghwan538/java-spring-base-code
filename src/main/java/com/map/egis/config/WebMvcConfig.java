package com.map.egis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String originalPath = asResourceLocation(storageProperties.originalDirectory());
        String compressedPath = asResourceLocation(storageProperties.compressedDirectory());

        log.info("Serving original images from {} and compressed images from {}", originalPath, compressedPath);

        registry.addResourceHandler("/images/original/**")
                .addResourceLocations(originalPath);

        registry.addResourceHandler("/images/compressed/**")
                .addResourceLocations(compressedPath);
    }

    private String asResourceLocation(Path directory) {
        String location = directory.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
