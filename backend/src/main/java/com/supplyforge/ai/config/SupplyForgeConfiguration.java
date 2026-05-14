package com.supplyforge.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class SupplyForgeConfiguration {

    @Bean
    RestClient geminiRestClient(GeminiProperties props) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutSeconds() * 1000);
        factory.setReadTimeout(props.getReadTimeoutSeconds() * 1000);

        var builder = RestClient.builder()
                .baseUrl(props.getBaseUrl().replaceAll("/$", ""))
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json");

        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            // Gemini uses x-goog-api-key header
            builder.defaultHeader("x-goog-api-key", props.getApiKey().trim());
        }

        return builder.build();
    }
}
