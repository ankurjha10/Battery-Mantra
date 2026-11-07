package com.api.batterymantra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcStaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:/var/www/batterymantra/uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Expose /uploads/** statically directly from filesystem
        String fileLocation = Paths.get(uploadDir).toUri().toString();
        if (!fileLocation.endsWith("/")) {
            fileLocation += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(fileLocation)
                .setCachePeriod(31536000); // 1 year cache header
    }
}

