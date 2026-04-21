package com.grupocinco.mrelote.carrito.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CarritoResponse(
        Long id,
        List<ItemCarritoResponse> items,
        BigDecimal tarifaEnvio,
        BigDecimal total,
        LocalDateTime ultimaActualizacion
) {}
