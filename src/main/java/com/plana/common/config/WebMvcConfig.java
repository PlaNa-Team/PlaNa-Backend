package com.plana.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    // REST API 백엔드 구현을 위한 중요한 설정 파일
    // CORS 설정, 컨트롤러 요청 매핑 및 변환 설정, 인터셉터 등록

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins( "http://localhost:5173","http://localhost:5174",
                        "http://localhost:80","http://localhost","http://localhost:443",
                        "http://hoonee-math.info","https://hoonee-maht.info")// Vue 프로젝트의 개발 서버 주소
                .allowedMethods("GET", "POST", "PUT", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition") // 파일 이름 추출 허용
                .allowCredentials(true);
    }
}
