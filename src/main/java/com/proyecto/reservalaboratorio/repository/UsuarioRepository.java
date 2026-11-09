package com.proyecto.reservalaboratorio.repository;

import java.util.Optional;

import com.proyecto.reservalaboratorio.entity.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

    @Query(name = "Usuario.buscarPorCorreo")
    Optional<Usuario> buscarPorCorreo(
        @Param("correo") String correo
    );
}
