package com.api.batterymantra.security;

import jakarta.servlet.http.HttpServletResponse;
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
@SuppressWarnings("null")
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final CanonicalRedirectFilter canonicalRedirectFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.deny()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write(
                                    "{\"message\":\"Full authentication is required to access this resource\",\"status\":401}"
                            );
                        })
                )
                .authorizeHttpRequests(requests -> requests

                        // Auth — public
                        .requestMatchers("/api/auth/**").permitAll()

                        // Callbacks — public for creation
                        .requestMatchers(HttpMethod.POST, "/api/callbacks").permitAll()

                        // Enquiries — public for creation
                        .requestMatchers(HttpMethod.POST, "/api/enquiries/**").permitAll()

                        // Banners — read public
                        .requestMatchers(HttpMethod.GET, "/api/banners", "/api/banners/**").permitAll()

                        // Reels — read public
                        .requestMatchers(HttpMethod.GET, "/api/reels", "/api/reels/**").permitAll()

                        // Public endpoints (e.g. delivery time)
                        .requestMatchers(HttpMethod.GET, "/api/public", "/api/public/**").permitAll()

                        // Campaigns & Offers — public storefront
                        .requestMatchers("/api/campaigns", "/api/campaigns/**").permitAll()

                        // Coupons — public storefront (active PDP display, apply)
                        .requestMatchers("/api/coupons/**").permitAll()

                        // Blogs — read public
                        .requestMatchers(HttpMethod.GET, "/api/blogs", "/api/blogs/**").permitAll()

                        // FAQs - public
                        .requestMatchers(HttpMethod.GET, "/api/faqs/public", "/api/faqs/public/**").permitAll()

                        // Locations — public
                        .requestMatchers(HttpMethod.GET, "/api/locations", "/api/locations/**").permitAll()

                        // Products — read public, write ADMIN (fine-grained via @PreAuthorize)
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("ADMIN", "SUB_ADMIN")

                        // Categories — read public, write ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/categories/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAnyRole("ADMIN", "SUB_ADMIN")

                        // Brands — read public, write ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/brands", "/api/brands/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/brands/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/brands/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/brands/**").hasAnyRole("ADMIN", "SUB_ADMIN")

                        // Brand Contents — read public
                        .requestMatchers(HttpMethod.GET, "/api/brand-contents", "/api/brand-contents/**").permitAll()

                        // Manufacturers — read public, write ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/manufacturers", "/api/manufacturers/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/manufacturers/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/manufacturers/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/manufacturers/**").hasAnyRole("ADMIN", "SUB_ADMIN")

                        // Vehicles — read public, write ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/vehicles", "/api/vehicles/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/vehicles/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/vehicles/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/vehicles/**").hasAnyRole("ADMIN", "SUB_ADMIN")

                        // Fuels — read public, write ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/fuels", "/api/fuels/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/fuels/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/fuels/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/fuels/**").hasAnyRole("ADMIN", "SUB_ADMIN")

                        // Cart — CUSTOMER only
                        .requestMatchers("/api/cart/**").hasRole("CUSTOMER")

                        // Address — CUSTOMER only
                        .requestMatchers("/api/address/**").hasRole("CUSTOMER")

                        // Payments — CUSTOMER and ENGINEER
                        .requestMatchers("/api/payments/**").hasAnyRole("CUSTOMER", "ENGINEER")

                        // Orders — CUSTOMER endpoints + ADMIN endpoints (fine-grained via @PreAuthorize)
                        .requestMatchers("/api/orders/admin/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        .requestMatchers("/api/orders/**").hasRole("CUSTOMER")

                        // Engineer App APIs
                        .requestMatchers("/api/engineer/**").hasRole("ENGINEER")

                        // Partner Dashboard APIs
                        .requestMatchers("/api/partner/**").hasRole("PARTNER")

                        // Super Admin Only Security APIs
                        .requestMatchers("/api/admin/security/**").hasRole("ADMIN")

                        // Admin APIs
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUB_ADMIN")

                        // User Profile & Settings
                        .requestMatchers("/api/user/**").authenticated()

                        // SEO & CMS - read public
                        .requestMatchers(HttpMethod.GET, "/api/seo", "/api/seo/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cms", "/api/cms/**").permitAll()

                        // Static uploaded files (local development fallback)
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/healthCheck"
                        ).permitAll()

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Full authentication is required to access this resource\",\"status\":401}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Access denied. You do not have permission to perform this action.\",\"status\":403}");
                        })
                )
                .addFilterBefore(canonicalRedirectFilter,
                        org.springframework.web.filter.CorsFilter.class)
                .addFilterBefore(rateLimitingFilter,
                        UsernamePasswordAuthenticationFilter.class)
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