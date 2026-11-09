package com.proyecto.reservalaboratorio.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(
            @Value("${app.jwt.secret}") String secret) {

        byte[] bytes = Base64.getDecoder().decode(secret);

        if (bytes.length < 32) {
            throw new IllegalArgumentException(
                "La clave JWT debe tener al menos 32 bytes"
            );
        }

        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
            .algorithm(MacAlgorithm.HS256)
            .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @Value("${app.jwt.issuer}") String issuer) {

        NimbusJwtDecoder decoder =
            NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer(issuer)
        );

        return decoder;
    }
}
