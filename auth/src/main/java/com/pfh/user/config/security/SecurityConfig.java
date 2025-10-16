package com.pfh.user.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Value("${security.csrf.enabled:false}")
    private boolean csrfEnabled;

    @Value("${security.cors.enabled:false}")
    private boolean corsEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (!csrfEnabled) {
            http.csrf(csrf -> csrf.disable());
        }

        return http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        System.out.println("corsEnabled: " + corsEnabled);
        if (corsEnabled) {
            registry.addMapping("/**")
                    .allowedOrigins("http://localhost:4321")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowCredentials(true);
        }
    }
}