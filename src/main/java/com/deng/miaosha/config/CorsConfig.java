package com.deng.miaosha.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();

        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", buildConfig());

        return new CorsFilter(urlBasedCorsConfigurationSource);
    }

    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        corsConfiguration.addAllowedOrigin("http://localhost:10000"); // 允许域名
        corsConfiguration.addAllowedOrigin("http://127.0.0.1:10000");

        corsConfiguration.setAllowCredentials(true); // 允许cookie

        corsConfiguration.addAllowedHeader("*"); // 允许头

        corsConfiguration.addAllowedMethod("*"); // 允许方法

        return corsConfiguration;
    }
}
