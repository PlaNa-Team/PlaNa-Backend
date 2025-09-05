package com.plana.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

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
                        "http://hoonee-math.info","https://hoonee-math.info",
                        "http://plana.hoonee-math.info","https://plana.hoonee-math.info")// 개발 서버 및 프로덕션 도메인
                .allowedMethods("GET", "POST", "PUT", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition") // 파일 이름 추출 허용
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/** 요청 → 프로젝트 루트/uploads/ 실제 파일 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
