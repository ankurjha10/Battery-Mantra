package com.api.batterymantra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests

                        // Auth — public
                        .requestMatchers("/api/auth/**").permitAll()

                        // Callbacks — public for creation
                        .requestMatchers(HttpMethod.POST, "/api/callbacks").permitAll()

                        // Banners — read public
                        .requestMatchers(HttpMethod.GET, "/api/banners/**").permitAll()

                        // Public endpoints (e.g. delivery time)
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()

                        // Locations — public
                        .requestMatchers(HttpMethod.GET, "/api/locations/**").permitAll()

                        // Products — read public, write ADMIN (fine-grained via @PreAuthorize)
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // Categories — read public, write ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                        // Brands — read public, write ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/brands/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/brands/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/brands/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/brands/**").hasRole("ADMIN")

                        // Manufacturers — read public, write ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/manufacturers/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/manufacturers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/manufacturers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/manufacturers/**").hasRole("ADMIN")

                        // Vehicles — read public, write ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/vehicles/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/vehicles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/vehicles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/vehicles/**").hasRole("ADMIN")

                        // Cart — CUSTOMER only
                        .requestMatchers("/api/cart/**").hasRole("CUSTOMER")

                        // Address — CUSTOMER only
                        .requestMatchers("/api/address/**").hasRole("CUSTOMER")

                        // Orders — CUSTOMER endpoints + ADMIN endpoints (fine-grained via @PreAuthorize)
                        .requestMatchers("/api/orders/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/orders/**").hasRole("CUSTOMER")

                        // Admin APIs
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // User Profile & Settings
                        .requestMatchers("/api/user/**").authenticated()

                        // SEO - read public
                        .requestMatchers(HttpMethod.GET, "/api/seo/**").permitAll()

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/healthCheck"
                        ).permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration auth) throws Exception {
        return auth.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}