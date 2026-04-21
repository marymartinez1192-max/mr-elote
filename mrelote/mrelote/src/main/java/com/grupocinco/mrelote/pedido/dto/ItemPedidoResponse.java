package com.grupocinco.mrelote.pedido.dto;

import com.grupocinco.mrelote.producto.dto.ProductoResponse;

import java.math.BigDecimal;

public record ItemPedidoResponse(
        Long id,
        ProductoResponse producto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {}
