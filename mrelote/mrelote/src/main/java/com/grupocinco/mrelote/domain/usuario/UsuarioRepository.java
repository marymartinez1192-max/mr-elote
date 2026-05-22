package com.grupocinco.mrelote.domain.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findBySupabaseUserId(UUID supabaseUserId);
    boolean existsByCorreo(String correo);
}
