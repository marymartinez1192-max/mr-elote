package com.grupocinco.mrelote.domain.carrito;

import com.grupocinco.mrelote.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    Optional<Carrito> findByUsuario(Usuario usuario);
}
