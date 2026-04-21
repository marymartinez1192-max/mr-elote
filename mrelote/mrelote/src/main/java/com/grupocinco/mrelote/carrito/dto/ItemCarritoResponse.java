package com.grupocinco.mrelote.carrito.dto;

import com.grupocinco.mrelote.producto.dto.ProductoResponse;

import java.math.BigDecimal;

public record ItemCarritoResponse(
        Long id,
        ProductoResponse producto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {}
