package com.grupocinco.mrelote.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ConfigNegocioRequest(
        @NotNull(message = "El horario de apertura es obligatorio")
        LocalTime horarioApertura,

        @NotNull(message = "El horario de cierre es obligatorio")
        LocalTime horarioCierre,

        @NotNull(message = "El estado de cierre manual es obligatorio")
        Boolean cerradoManual
) {}
