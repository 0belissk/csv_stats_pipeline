package com.paul.csvpipeline.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Disable CSRF (required for stateless APIs)
                .csrf(csrf -> csrf.disable())

                // Stateless session (JWT-ready)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/h2-console/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Allow H2 console to render properly
                .headers(headers ->
                        headers.frameOptions(frame -> frame.disable())
                )

                // Disable all default auth mechanisms
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable());


        return http.build();
    }
}
