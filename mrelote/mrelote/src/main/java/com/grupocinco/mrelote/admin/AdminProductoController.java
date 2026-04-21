package com.grupocinco.mrelote.admin;

import com.grupocinco.mrelote.producto.ProductoService;
import com.grupocinco.mrelote.producto.dto.ProductoDisponibilidadRequest;
import com.grupocinco.mrelote.producto.dto.ProductoRequest;
import com.grupocinco.mrelote.producto.dto.ProductoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductoController {

    private final ProductoService productoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return productoService.crear(request);
    }

    @PutMapping("/{productId}")
    public ProductoResponse actualizar(@PathVariable Long productId,
                                       @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(productId, request);
    }

    @PatchMapping("/{productId}/availability")
    public ProductoResponse actualizarDisponibilidad(@PathVariable Long productId,
                                                     @Valid @RequestBody ProductoDisponibilidadRequest request) {
        return productoService.actualizarDisponibilidad(productId, request);
    }
}
