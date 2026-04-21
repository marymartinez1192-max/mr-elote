package com.grupocinco.mrelote.domain.carrito;

import com.grupocinco.mrelote.domain.producto.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {
    Optional<ItemCarrito> findByCarritoAndProducto(Carrito carrito, Producto producto);
}
