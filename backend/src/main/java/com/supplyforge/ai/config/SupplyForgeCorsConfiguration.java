package com.supplyforge.ai.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SupplyForgeCorsConfiguration {

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${supplyforge.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
        CorsConfiguration c = new CorsConfiguration();
        List<String> origins =
                List.of(allowedOrigins.split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (origins.isEmpty()) {
            c.addAllowedOriginPattern("*");
        } else {
            origins.forEach(c::addAllowedOriginPattern);
        }
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}
