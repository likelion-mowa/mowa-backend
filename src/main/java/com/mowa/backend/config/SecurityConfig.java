package com.mowa.backend.config;

import com.mowa.backend.security.JwtAuthenticationEntryPoint;
import com.mowa.backend.security.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final List<String> corsAllowedOrigins;

    /**
     * 배포 환경에서는 CORS_ALLOWED_ORIGINS 환경 변수(또는 cors.allowed-origins 프로퍼티)로
     * 이 목록을 덮어쓴다. 기본값은 로컬 개발과 Vercel 배포를 모두 포함한다.
     *
     * 8081은 Expo Web 기본 포트, 8082는 8081이 이미 점유됐을 때 Expo가 쓰는 대체 포트다.
     * Vercel은 프로덕션 도메인 외에 PR마다 walk-diary-frontend-<해시>.vercel.app 형태의
     * 프리뷰 도메인을 새로 만들기 때문에 고정 문자열로는 감당할 수 없어 패턴을 쓴다.
     */
    public SecurityConfig(
            @Value("${cors.allowed-origins:"
                    + "http://localhost:8081,"
                    + "http://localhost:8082,"
                    + "http://localhost:19006,"
                    + "https://walk-diary-frontend.vercel.app,"
                    + "https://walk-diary-frontend-*.vercel.app}")
            List<String> corsAllowedOrigins
    ) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
    ) throws Exception {
        return http
                // corsConfigurationSource 빈을 실제로 적용하는 유일한 스위치다.
                // 이 줄이 없으면 빈이 존재해도 Spring Security가 무시하고,
                // 응답에 Access-Control-* 헤더가 하나도 붙지 않는다.
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll()
                )
                // preflight(OPTIONS)에는 Authorization 헤더가 없다. 위 .cors()가 등록하는
                // CorsFilter가 이 필터보다 앞에서 preflight를 끝내므로 인증 규칙에
                // OPTIONS 예외를 따로 둘 필요가 없다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 웹 클라이언트는 백엔드와 Origin이 달라 CORS 없이는 로그인부터 차단된다.
     * 로컬 개발도 마찬가지다 - localhost:8081과 localhost:8080은 포트가 달라 이미 교차 출처다.
     *
     * iOS 네이티브 fetch는 CORS 검사를 받지 않으므로 이 설정이 없어도 동작한다.
     * 즉 iOS에서만 검증하면 이 결함이 드러나지 않는다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // setAllowedOrigins가 아니라 패턴 API를 쓴다. 와일드카드가 없는 항목은
        // 정확히 일치할 때만 통과하므로 고정 도메인의 안전성은 그대로다.
        configuration.setAllowedOriginPatterns(corsAllowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        // Access Token을 Authorization 헤더로만 전달하고 쿠키를 쓰지 않으므로 자격 증명을
        // 허용하지 않는다. 브라우저가 인증 정보를 자동으로 실어 보내지 않는다는 뜻이라
        // 헤더 목록을 "*"로 열어도 토큰 없는 타 사이트는 아무것도 읽지 못한다.
        // 반대로 목록을 손으로 관리하면 헤더 하나 빠질 때마다 preflight가 조용히 깨진다.
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        // 같은 엔드포인트에 대한 preflight를 1시간 동안 재사용한다.
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 클라이언트가 호출하는 경로는 /api/v1 뿐이다. Swagger 등 나머지 경로는
        // 교차 출처로 열 이유가 없다.
        source.registerCorsConfiguration("/api/v1/**", configuration);
        return source;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:[*]",
                "http://127.0.0.1:[*]"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", configuration);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
