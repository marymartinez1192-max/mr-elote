package com.grupocinco.mrelote.pedido.dto;

import com.grupocinco.mrelote.domain.pedido.EstadoPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        EstadoPedido estado,
        List<ItemPedidoResponse> items,
        BigDecimal tarifaEnvio,
        BigDecimal total,
        LocalDateTime fechaCreacion
) {}
