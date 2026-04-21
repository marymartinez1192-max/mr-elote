package com.grupocinco.mrelote.domain.producto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByCategoriaId(Long categoriaId);
    List<Producto> findByDisponible(boolean disponible);
    List<Producto> findByCategoriaIdAndDisponible(Long categoriaId, boolean disponible);
}
