package com.nikyfarm.config;

import com.nikyfarm.security.JsonAccessDeniedHandler;
import com.nikyfarm.security.JsonAuthenticationEntryPoint;
import com.nikyfarm.security.JwtAuthenticationFilter;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for the NikyFarm application.
 *
 * <p>The application has two trust zones:
 *
 * <ul>
 *   <li>/admin/** - server-rendered Thymeleaf admin panel
 *   <li>/api/** - JSON REST API consumed by Flutter and other clients
 * </ul>
 *
 * <p>CORS origins are configured externally so that local development and production can use
 * different origin policies without modifying Java source code.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginPatterns;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        handling ->
                                handling.authenticationEntryPoint(
                                                jsonAuthenticationEntryPoint)
                                        .accessDeniedHandler(jsonAccessDeniedHandler))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/admin/**",
                                                "/css/**",
                                                "/js/**",
                                                "/favicon.png",
                                                "/",
                                                "/error")
                                        .permitAll()
                                        .requestMatchers("/api/auth/**")
                                        .permitAll()
                                        .requestMatchers("/api/payments/webhook")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/categories/**",
                                                "/api/products/**",
                                                "/api/attributes/**",
                                                "/api/review-images/*")
                                        .permitAll()
                                        .requestMatchers("/api/me/**")
                                        .authenticated()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/products/*/reviews")
                                        .authenticated()
                                        .requestMatchers(
                                                HttpMethod.PUT,
                                                "/api/reviews/**")
                                        .authenticated()
                                        .requestMatchers(
                                                HttpMethod.DELETE,
                                                "/api/reviews/**")
                                        .authenticated()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/review-images")
                                        .authenticated()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/users/me")
                                        .authenticated()
                                        .requestMatchers(
                                                HttpMethod.PUT,
                                                "/api/users/me")
                                        .authenticated()
                                        .requestMatchers("/api/addresses/**")
                                        .authenticated()
                                        .requestMatchers("/api/users/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins =
                Arrays.stream(allowedOriginPatterns.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isBlank())
                        .toList();

        configuration.setAllowedOriginPatterns(origins);

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}