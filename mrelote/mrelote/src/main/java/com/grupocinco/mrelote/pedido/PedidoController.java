package com.grupocinco.mrelote.pedido;

import com.grupocinco.mrelote.auth.CurrentUser;
import com.grupocinco.mrelote.domain.usuario.Usuario;
import com.grupocinco.mrelote.pedido.dto.PedidoPageResponse;
import com.grupocinco.mrelote.pedido.dto.PedidoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse confirmar(@CurrentUser Usuario usuario) {
        return pedidoService.confirmar(usuario);
    }

    @GetMapping
    public PedidoPageResponse listar(@CurrentUser Usuario usuario,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return pedidoService.listar(usuario, page, size);
    }

    @GetMapping("/{orderId}")
    public PedidoResponse obtener(@CurrentUser Usuario usuario,
                                  @PathVariable Long orderId) {
        return pedidoService.obtener(usuario, orderId);
    }

    @PatchMapping("/{orderId}/cancel")
    public PedidoResponse cancelar(@CurrentUser Usuario usuario,
                                   @PathVariable Long orderId) {
        return pedidoService.cancelar(usuario, orderId);
    }
}
