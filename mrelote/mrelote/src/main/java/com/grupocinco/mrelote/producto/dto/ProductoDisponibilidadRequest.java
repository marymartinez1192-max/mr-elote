package com.grupocinco.mrelote.producto.dto;

import jakarta.validation.constraints.NotNull;

public record ProductoDisponibilidadRequest(
        @NotNull(message = "La disponibilidad es obligatoria")
        Boolean disponible
) {}
