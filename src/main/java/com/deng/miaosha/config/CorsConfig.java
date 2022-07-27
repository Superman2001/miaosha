package com.deng.miaosha.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/*
 由于本地开发时前后端分离且不在同一端口,会使用ajax发起跨域请求,需要使用该配置
 但在生产环境中会使用nginx作反向代理，前后端在同一端口，不需要使用该配置
*/
@Profile("dev")
//配置跨域相关的属性
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

        //当AllowCredentials=true时,AllowedOrigin不能为 *
//        corsConfiguration.addAllowedOrigin("http://127.0.0.1:8088"); // 允许域名
        corsConfiguration.addAllowedOrigin("*");

        //需要和前端的xhrFields:{withCredentials:true}配合使用
//        corsConfiguration.setAllowCredentials(true); // 允许cookie

        corsConfiguration.addAllowedHeader("*"); // 允许头

        corsConfiguration.addAllowedMethod("*"); // 允许方法

        return corsConfiguration;
    }
}
