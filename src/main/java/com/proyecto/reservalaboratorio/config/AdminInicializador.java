package com.proyecto.reservalaboratorio.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.proyecto.reservalaboratorio.dto.UsuarioCrearRequest;
import com.proyecto.reservalaboratorio.entity.Rol;
import com.proyecto.reservalaboratorio.entity.Usuario;
import com.proyecto.reservalaboratorio.repository.UsuarioRepository;

import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
    name = "app.bootstrap.enabled",
    havingValue = "true"
)
public class AdminInicializador implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final String correo;
    private final String password;

    public AdminInicializador(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            Validator validator,
            @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String correo,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
        this.correo = correo;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {

        boolean existeAdministrador = usuarioRepository.findAll()
            .stream()
            .anyMatch(usuario ->
                usuario.getRol() == Rol.ADMINISTRADOR
            );

        if (existeAdministrador) {
            System.out.println(
                "Inicialización omitida: ya existe un administrador."
            );
            return;
        }

        String correoNormalizado = correo.trim()
            .toLowerCase(Locale.ROOT);

        UsuarioCrearRequest datos = new UsuarioCrearRequest(
            "Administrador",
            correoNormalizado,
            password
        );

        if (!validator.validate(datos).isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new IllegalStateException(
                "Revisa el correo y la contraseña del administrador: "
                + "correo válido y contraseña de 12 a 64 caracteres, "
                + "con un máximo de 72 bytes."
            );
        }

        if (usuarioRepository
                .buscarPorCorreo(correoNormalizado).isPresent()) {

            throw new IllegalStateException(
                "El correo elegido ya pertenece a otro usuario. "
                + "Usa un correo diferente para el administrador."
            );
        }

        Usuario administrador = new Usuario();

        administrador.setNombre("Administrador");
        administrador.setCorreo(correoNormalizado);
        administrador.setActivo(true);
        administrador.setRol(Rol.ADMINISTRADOR);
        administrador.setPasswordHash(
            passwordEncoder.encode(password)
        );

        usuarioRepository.saveAndFlush(administrador);

        System.out.println(
            "Cuenta de administrador creada correctamente."
        );
    }
}
