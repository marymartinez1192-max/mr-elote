package com.grupocinco.mrelote.admin;

import com.grupocinco.mrelote.admin.dto.ActualizarEstadoPedidoRequest;
import com.grupocinco.mrelote.domain.pedido.EstadoPedido;
import com.grupocinco.mrelote.pedido.dto.PedidoPageResponse;
import com.grupocinco.mrelote.pedido.dto.PedidoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminPedidoController {

    private final AdminPedidoService adminPedidoService;

    @GetMapping
    public PedidoPageResponse listar(
            @RequestParam(required = false) EstadoPedido status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminPedidoService.listarTodos(status, page, size);
    }

    @PatchMapping("/{orderId}/status")
    public PedidoResponse actualizarEstado(@PathVariable Long orderId,
                                           @Valid @RequestBody ActualizarEstadoPedidoRequest request) {
        return adminPedidoService.actualizarEstado(orderId, request);
    }
}
