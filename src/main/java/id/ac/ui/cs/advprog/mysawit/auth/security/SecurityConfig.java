package id.ac.ui.cs.advprog.mysawit.auth.security;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        /**
         * Chain 1 (highest priority): handles all public endpoints and OPTIONS preflight.
         * No JWT filter is added — requests that match this chain are permitted unconditionally.
         * Uses getRequestURI() directly to avoid getServletPath() issues behind Koyeb/Cloudflare.
         */
        @Bean
        @Order(1)
        public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher(request -> {
                                        String uri = request.getRequestURI();
                                        String method = request.getMethod();
                                        return "OPTIONS".equalsIgnoreCase(method)
                                                || uri.startsWith("/api/auth/register")
                                                || uri.startsWith("/api/auth/login")
                                                || uri.startsWith("/api/auth/google-login")
                                                || uri.startsWith("/api/auth/logout")
                                                || uri.startsWith("/api/auth/profile/");
                                })
                                .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
                return http.build();
        }

        /**
         * Chain 2: handles all protected endpoints.
         * JWT filter runs here. All requests require authentication unless matched above.
         */
        @Bean
        @Order(2)
        public SecurityFilterChain securedFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/admin/**")).hasRole("ADMIN")
                                                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/me")).authenticated()
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.setContentType("application/json");
                                                        response.getWriter().write(
                                                                "{\"message\":\"Unauthorized: " + authException.getMessage() + "\"}");
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                                        response.setContentType("application/json");
                                                        response.getWriter().write(
                                                                "{\"message\":\"Forbidden: insufficient permissions\"}");
                                                }))
                                .addFilterBefore(jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // allowedOriginPatterns("*") works with credentials and covers all
                // deployment URLs (Vercel previews, custom domains, localhost, etc.)
                configuration.setAllowedOriginPatterns(List.of("*"));
                configuration.setAllowedMethods(List.of(
                                "GET",
                                "POST",
                                "PUT",
                                "DELETE",
                                "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
