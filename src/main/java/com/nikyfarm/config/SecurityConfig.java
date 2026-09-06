package com.nikyfarm.config;

import com.nikyfarm.security.JsonAccessDeniedHandler;
import com.nikyfarm.security.JsonAuthenticationEntryPoint;
import com.nikyfarm.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

/**
 * Two very different trust zones share this app:
 *  - {@code /admin/**} — the server-rendered Thymeleaf admin panel. Left fully open
 *    (permitAll). It has no login page or cookie/session plumbing; bearer-token JWT
 *    doesn't apply to a multi-page HTML app without a materially different mechanism
 *    (JWT-in-cookie + a login form). That's a separate future task.
 *  - {@code /api/**} — the JSON REST API consumed by an external client (see the
 *    Flutter dev-server CORS origin below). This is where real JWT auth applies:
 *    public catalog reads, authenticated actions on your own data, admin-only writes.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Admin panel + static assets — open, see class Javadoc.
                        .requestMatchers("/admin/**", "/css/**", "/js/**", "/favicon.png", "/", "/error").permitAll()

                        // Auth endpoints — open, that's the point.
                        .requestMatchers("/api/auth/**").permitAll()

                        // Public catalog browsing (GET only — writes are locked down further below).
                        // "/api/review-images/*" (single segment) deliberately excludes the bare
                        // "/api/review-images" list, which is the admin moderation endpoint.
                        .requestMatchers(HttpMethod.GET,
                                "/api/categories/**", "/api/products/**", "/api/attributes/**", "/api/review-images/*")
                        .permitAll()

                        // Acting on your own account/wishlist/cart — just needs to be logged in.
                        .requestMatchers("/api/me/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/products/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/review-images").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()
                        .requestMatchers("/api/addresses/**").authenticated()

                        // Everything else under /api/users/** is admin user-management.
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // Everything else under /api/** (catalog writes, review moderation) is admin-only.
                        .requestMatchers("/api/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS = which origins (domains) are allowed to call this API from a browser.
     * Does NOT affect Postman — Postman requests always go through regardless of this config.
     * This matters once you build a frontend (React/Vue/etc.) running on a different
     * origin (e.g. localhost:3000) than your Spring Boot backend (e.g. localhost:8080).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // TODO: replace with your actual frontend URL(s) once deployed.
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",   // local frontend dev server (adjust port as needed)
                "http://localhost:5173",   // vite frontend dev server
                "http://localhost:38790"   // Flutter default dev server
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);  // needed if you'll send cookies/auth headers later

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
