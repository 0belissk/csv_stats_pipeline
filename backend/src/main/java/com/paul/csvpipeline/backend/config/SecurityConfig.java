package com.paul.csvpipeline.backend.config;

import com.paul.csvpipeline.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }


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
                                "/h2-console/**",
                                "/auth/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Allow H2 console to render properly
                .headers(headers ->
                        headers.frameOptions(frame -> frame.disable())
                )

                // Disable all default auth mechanisms
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())

                // JWT validation filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);



        return http.build();
    }
}
