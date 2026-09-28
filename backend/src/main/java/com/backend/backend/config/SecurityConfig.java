package com.backend.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration 
public class SecurityConfig {
    
    @Bean 
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}

/**
 * Placeholder security config for the MVP.
 *
 * The Telegram bot talks to Telegram via long-polling, not via an
 * inbound HTTP endpoint, so there is nothing to protect yet on that
 * path. This permits all requests so Spring Security's default
 * generated-password login doesn't block local development.
 *
 * TODO before Version 4 (admin dashboard): replace this with real
 * authentication/authorization for the admin API, and add a payment
 * webhook endpoint secured by signature verification rather than
 * permit-all.
 */
