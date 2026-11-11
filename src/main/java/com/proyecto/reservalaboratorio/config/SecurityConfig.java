package com.proyecto.reservalaboratorio.config;

import java.util.List;


import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ErroresSeguridadHandler erroresSeguridad)
            throws Exception {

        http
            .cors(Customizer.withDefaults())

            // La API utilizará tokens Bearer, sin cookies de sesión.
            .csrf(AbstractHttpConfigurer::disable)

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)

            .authorizeHttpRequests(auth -> auth

                // Permite conservar las respuestas de error originales.
                .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()

                .requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()

                .requestMatchers("/api/usuarios", "/api/usuarios/**")
                    .hasRole("ADMINISTRADOR")

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/laboratorios",
                    "/api/laboratorios/**"
                )
                    .hasAnyRole("ADMINISTRADOR", "SOLICITANTE")

                .requestMatchers(
                    "/api/laboratorios",
                    "/api/laboratorios/**"
                )
                    .hasRole("ADMINISTRADOR")

                .requestMatchers(HttpMethod.GET, "/api/reservas/indicadores")
                    .hasRole("ADMINISTRADOR")

                // El servicio comprueba que el solicitante solo
                // acceda a sus propias reservas.
                .requestMatchers("/api/reservas", "/api/reservas/**")
                    .hasAnyRole("ADMINISTRADOR", "SOLICITANTE")

                .anyRequest().denyAll()
            )

            // Respuestas 401 y 403 en formato JSON.
            .exceptionHandling(errores -> errores
                .authenticationEntryPoint(erroresSeguridad)
                .accessDeniedHandler(erroresSeguridad)
            )

            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(erroresSeguridad)
                .accessDeniedHandler(erroresSeguridad)
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(
                        jwtAuthenticationConverter
                    )
                )
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter roles =
            new JwtGrantedAuthoritiesConverter();

        roles.setAuthoritiesClaimName("rol");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(roles);

        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration cors = new CorsConfiguration();

        cors.setAllowedOrigins(List.of("http://localhost:4200"));

        cors.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        cors.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "Accept"
        ));

        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", cors);

        return source;
    }
}
