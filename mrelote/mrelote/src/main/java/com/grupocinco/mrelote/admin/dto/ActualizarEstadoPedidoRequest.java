package com.grupocinco.mrelote.admin.dto;

import com.grupocinco.mrelote.domain.pedido.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoPedidoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPedido estado
) {}
