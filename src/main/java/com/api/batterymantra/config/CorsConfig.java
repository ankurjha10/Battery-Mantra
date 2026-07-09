package com.api.batterymantra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowedOrigins:http://localhost:5173}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Use patterns to support Vercel wildcard subdomains (e.g., https://*.vercel.app)
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE",
                "PATCH", "OPTIONS"
        ));
        config.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type",
                "Accept", "Origin", "X-Requested-With"
        ));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}