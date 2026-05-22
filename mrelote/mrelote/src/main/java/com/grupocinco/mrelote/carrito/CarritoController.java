package com.grupocinco.mrelote.carrito;

import com.grupocinco.mrelote.auth.CurrentUser;
import com.grupocinco.mrelote.carrito.dto.ActualizarItemRequest;
import com.grupocinco.mrelote.carrito.dto.AgregarItemRequest;
import com.grupocinco.mrelote.carrito.dto.CarritoResponse;
import com.grupocinco.mrelote.domain.usuario.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService carritoService;

    @GetMapping
    public CarritoResponse obtener(@CurrentUser Usuario usuario) {
        return carritoService.obtener(usuario);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CarritoResponse agregarItem(@CurrentUser Usuario usuario,
                                       @Valid @RequestBody AgregarItemRequest request) {
        return carritoService.agregarItem(usuario, request);
    }

    @PutMapping("/items/{productoId}")
    public CarritoResponse actualizarItem(@CurrentUser Usuario usuario,
                                          @PathVariable Long productoId,
                                          @Valid @RequestBody ActualizarItemRequest request) {
        return carritoService.actualizarItem(usuario, productoId, request);
    }

    @DeleteMapping("/items/{itemId}")
    public CarritoResponse eliminarItem(@CurrentUser Usuario usuario,
                                        @PathVariable Long itemId) {
        return carritoService.eliminarItem(usuario, itemId);
    }
}
