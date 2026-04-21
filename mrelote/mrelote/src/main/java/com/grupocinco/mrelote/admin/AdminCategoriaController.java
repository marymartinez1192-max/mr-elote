package com.grupocinco.mrelote.admin;

import com.grupocinco.mrelote.producto.ProductoService;
import com.grupocinco.mrelote.producto.dto.CategoriaRequest;
import com.grupocinco.mrelote.producto.dto.CategoriaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoriaController {

    private final ProductoService productoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest request) {
        return productoService.crearCategoria(request);
    }

    @PutMapping("/{categoriaId}")
    public CategoriaResponse actualizar(@PathVariable Long categoriaId,
                                        @Valid @RequestBody CategoriaRequest request) {
        return productoService.actualizarCategoria(categoriaId, request);
    }

    @DeleteMapping("/{categoriaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long categoriaId) {
        productoService.eliminarCategoria(categoriaId);
    }
}
