package com.grupocinco.mrelote.domain.negocio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigNegocioRepository extends JpaRepository<ConfigNegocio, Long> {
    Optional<ConfigNegocio> findFirstBy();
}
