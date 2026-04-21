package com.grupocinco.mrelote.catalogo;

import com.grupocinco.mrelote.producto.ProductoService;
import com.grupocinco.mrelote.producto.dto.CategoriaResponse;
import com.grupocinco.mrelote.producto.dto.ProductoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CatalogoController {

    private final ProductoService productoService;

    @GetMapping("/categories")
    public List<CategoriaResponse> listarCategorias() {
        return productoService.listarCategorias();
    }

    @GetMapping("/products")
    public List<ProductoResponse> listarProductos(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean available) {
        return productoService.listarProductos(categoryId, available);
    }
}
