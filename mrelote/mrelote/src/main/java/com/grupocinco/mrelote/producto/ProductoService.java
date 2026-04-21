package com.grupocinco.mrelote.producto;

import com.grupocinco.mrelote.domain.categoria.Categoria;
import com.grupocinco.mrelote.domain.categoria.CategoriaRepository;
import com.grupocinco.mrelote.domain.producto.Producto;
import com.grupocinco.mrelote.domain.producto.ProductoRepository;
import com.grupocinco.mrelote.exception.RecursoNoEncontradoException;
import com.grupocinco.mrelote.producto.dto.CategoriaRequest;
import com.grupocinco.mrelote.producto.dto.CategoriaResponse;
import com.grupocinco.mrelote.producto.dto.ProductoDisponibilidadRequest;
import com.grupocinco.mrelote.producto.dto.ProductoRequest;
import com.grupocinco.mrelote.producto.dto.ProductoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public List<CategoriaResponse> listarCategorias() {
        return categoriaRepository.findAll().stream()
                .map(c -> new CategoriaResponse(c.getId(), c.getNombre()))
                .toList();
    }

    public CategoriaResponse crearCategoria(CategoriaRequest request) {
        Categoria categoria = new Categoria();
        categoria.setNombre(request.nombre());
        return toCategoriaResponse(categoriaRepository.save(categoria));
    }

    public CategoriaResponse actualizarCategoria(Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria", id));
        categoria.setNombre(request.nombre());
        return toCategoriaResponse(categoriaRepository.save(categoria));
    }

    public void eliminarCategoria(Long id) {
        categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria", id));
        if (!productoRepository.findByCategoriaId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar la categoría porque tiene productos asociados");
        }
        categoriaRepository.deleteById(id);
    }

    private CategoriaResponse toCategoriaResponse(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre());
    }

    public List<ProductoResponse> listarProductos(Long categoriaId, Boolean disponible) {
        List<Producto> productos;

        if (categoriaId != null && disponible != null) {
            productos = productoRepository.findByCategoriaIdAndDisponible(categoriaId, disponible);
        } else if (categoriaId != null) {
            productos = productoRepository.findByCategoriaId(categoriaId);
        } else if (disponible != null) {
            productos = productoRepository.findByDisponible(disponible);
        } else {
            productos = productoRepository.findAll();
        }

        return productos.stream().map(this::toResponse).toList();
    }

    public ProductoResponse crear(ProductoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria", request.categoriaId()));

        Producto producto = new Producto();
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setImagenUrl(request.imagenUrl());
        producto.setDisponible(request.disponible());
        producto.setCategoria(categoria);

        return toResponse(productoRepository.save(producto));
    }

    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria", request.categoriaId()));

        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setImagenUrl(request.imagenUrl());
        producto.setDisponible(request.disponible());
        producto.setCategoria(categoria);

        return toResponse(productoRepository.save(producto));
    }

    public ProductoResponse actualizarDisponibilidad(Long id, ProductoDisponibilidadRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));

        producto.setDisponible(request.disponible());

        return toResponse(productoRepository.save(producto));
    }

    private ProductoResponse toResponse(Producto p) {
        CategoriaResponse cat = new CategoriaResponse(p.getCategoria().getId(), p.getCategoria().getNombre());
        return new ProductoResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.getImagenUrl(), p.isDisponible(), cat);
    }
}
