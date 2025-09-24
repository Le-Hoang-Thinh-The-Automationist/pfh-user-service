package com.pfh.user.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${security.csrf.enabled:false}")
    private boolean csrfEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (!csrfEnabled) {
            http.csrf(csrf -> csrf.disable());
        }

        return http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }
}