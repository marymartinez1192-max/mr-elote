package com.grupocinco.mrelote.producto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo")
        BigDecimal precio,

        String imagenUrl,

        @NotNull(message = "La disponibilidad es obligatoria")
        Boolean disponible,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId
) {}
