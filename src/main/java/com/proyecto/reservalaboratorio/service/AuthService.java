package com.proyecto.reservalaboratorio.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

import com.proyecto.reservalaboratorio.dto.LoginRequest;
import com.proyecto.reservalaboratorio.dto.LoginResponse;
import com.proyecto.reservalaboratorio.entity.Usuario;
import com.proyecto.reservalaboratorio.repository.UsuarioRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirationSeconds;
    private final String hashSimulado;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;

        this.hashSimulado = passwordEncoder.encode(
            java.util.UUID.randomUUID().toString()
        );

        if (expirationSeconds <= 0 || expirationSeconds > 3600) {
            throw new IllegalArgumentException(
                "La duración del token debe estar entre 1 y 3600 segundos"
            );
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(@NotNull @Valid LoginRequest datos) {

        String correo = datos.correo()
            .trim()
            .toLowerCase(Locale.ROOT);

        if (datos.password()
                .getBytes(StandardCharsets.UTF_8).length > 72) {
            throw credencialesInvalidas();
        }

        Usuario usuario = usuarioRepository
            .buscarPorCorreo(correo)
            .orElse(null);

        String hash = usuario != null
                && usuario.getPasswordHash() != null
                && !usuario.getPasswordHash().isBlank()
            ? usuario.getPasswordHash()
            : hashSimulado;

        boolean passwordCorrecto =
            passwordEncoder.matches(datos.password(), hash);

        if (usuario == null
                || !passwordCorrecto
                || usuario.getPasswordHash() == null
                || usuario.getPasswordHash().isBlank()
                || !Boolean.TRUE.equals(usuario.getActivo())
                || usuario.getRol() == null) {

            throw credencialesInvalidas();
        }

        Instant ahora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(usuario.getId().toString())
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(expirationSeconds))
            .claim("rol", usuario.getRol().name())
            .build();

        JwsHeader header = JwsHeader
            .with(MacAlgorithm.HS256)
            .build();

        String token = jwtEncoder.encode(
            JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        return new LoginResponse(
            token,
            "Bearer",
            expirationSeconds,
            usuario.getId(),
            usuario.getNombre(),
            usuario.getRol().name()
        );
    }

    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Correo o contraseña incorrectos"
        );
    }
}
