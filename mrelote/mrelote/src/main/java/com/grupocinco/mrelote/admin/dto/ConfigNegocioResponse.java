package com.grupocinco.mrelote.admin.dto;

import java.time.LocalTime;

public record ConfigNegocioResponse(
        Long id,
        LocalTime horarioApertura,
        LocalTime horarioCierre,
        boolean cerradoManual
) {}
