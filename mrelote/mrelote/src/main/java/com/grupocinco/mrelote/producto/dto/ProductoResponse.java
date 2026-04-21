package com.grupocinco.mrelote.producto.dto;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        String imagenUrl,
        boolean disponible,
        CategoriaResponse categoria
) {}
