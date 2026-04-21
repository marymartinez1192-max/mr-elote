package com.grupocinco.mrelote.carrito.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarItemRequest(
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1")
        Integer cantidad
) {}
