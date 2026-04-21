package com.grupocinco.mrelote.pedido.dto;

import java.util.List;

public record PedidoPageResponse(
        List<PedidoResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {}
