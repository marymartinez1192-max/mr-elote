package com.grupocinco.mrelote.carrito;

import com.grupocinco.mrelote.carrito.dto.ActualizarItemRequest;
import com.grupocinco.mrelote.carrito.dto.AgregarItemRequest;
import com.grupocinco.mrelote.carrito.dto.CarritoResponse;
import com.grupocinco.mrelote.carrito.dto.ItemCarritoResponse;
import com.grupocinco.mrelote.config.ZonaNegocio;
import com.grupocinco.mrelote.domain.carrito.Carrito;
import com.grupocinco.mrelote.domain.carrito.CarritoRepository;
import com.grupocinco.mrelote.domain.carrito.ItemCarrito;
import com.grupocinco.mrelote.domain.carrito.ItemCarritoRepository;
import com.grupocinco.mrelote.domain.producto.Producto;
import com.grupocinco.mrelote.domain.producto.ProductoRepository;
import com.grupocinco.mrelote.domain.usuario.Usuario;
import com.grupocinco.mrelote.exception.AccesoDenegadoException;
import com.grupocinco.mrelote.exception.ProductoNoDisponibleException;
import com.grupocinco.mrelote.exception.RecursoNoEncontradoException;
import com.grupocinco.mrelote.producto.dto.CategoriaResponse;
import com.grupocinco.mrelote.producto.dto.ProductoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final ProductoRepository productoRepository;

    @Value("${app.carrito.tarifa-envio:3000}")
    private BigDecimal tarifaEnvio;

    public CarritoResponse obtener(Usuario usuario) {
        Carrito carrito = carritoRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Carrito", usuario.getId()));
        return toResponse(carrito);
    }

    @Transactional
    public CarritoResponse agregarItem(Usuario usuario, AgregarItemRequest request) {
        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", request.productoId()));

        // RN-02: no se pueden agregar productos no disponibles
        if (!producto.isDisponible()) {
            throw new ProductoNoDisponibleException(producto.getNombre());
        }

        // RN-01: obtener o crear el único carrito activo del usuario
        Carrito carrito = carritoRepository.findByUsuario(usuario).orElseGet(() -> {
            Carrito nuevo = new Carrito();
            nuevo.setUsuario(usuario);
            return carritoRepository.save(nuevo);
        });

        // Si el producto ya está en el carrito, suma la cantidad
        ItemCarrito item = itemCarritoRepository.findByCarritoAndProducto(carrito, producto)
                .orElseGet(() -> {
                    ItemCarrito nuevo = new ItemCarrito();
                    nuevo.setCarrito(carrito);
                    nuevo.setProducto(producto);
                    nuevo.setPrecioUnitario(producto.getPrecio());
                    nuevo.setCantidad(0);
                    nuevo.setSubtotal(BigDecimal.ZERO);
                    return nuevo;
                });

        item.setCantidad(item.getCantidad() + request.cantidad());
        item.setSubtotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())));
        ItemCarrito itemGuardado = itemCarritoRepository.save(item);

        if (!carrito.getItems().contains(itemGuardado)) {
            carrito.getItems().add(itemGuardado);
        }

        carrito.setUltimaActualizacion(ZonaNegocio.ahora());
        return toResponse(carritoRepository.save(carrito));
    }

    @Transactional
    public CarritoResponse actualizarItem(Usuario usuario, Long productoId, ActualizarItemRequest request) {
        Carrito carrito = carritoRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Carrito", usuario.getId()));

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", productoId));

        ItemCarrito item = itemCarritoRepository.findByCarritoAndProducto(carrito, producto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto en carrito", productoId));

        item.setCantidad(request.cantidad());
        item.setSubtotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(request.cantidad())));
        itemCarritoRepository.save(item);

        carrito.setUltimaActualizacion(ZonaNegocio.ahora());
        return toResponse(carritoRepository.save(carrito));
    }

    @Transactional
    public CarritoResponse eliminarItem(Usuario usuario, Long itemId) {
        ItemCarrito item = itemCarritoRepository.findById(itemId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Item", itemId));

        validarPropietario(item.getCarrito(), usuario);

        Carrito carrito = item.getCarrito();
        carrito.getItems().remove(item);
        itemCarritoRepository.delete(item);

        carrito.setUltimaActualizacion(ZonaNegocio.ahora());
        return toResponse(carritoRepository.save(carrito));
    }

    private void validarPropietario(Carrito carrito, Usuario usuario) {
        if (!carrito.getUsuario().getId().equals(usuario.getId())) {
            throw new AccesoDenegadoException();
        }
    }

    private CarritoResponse toResponse(Carrito carrito) {
        List<ItemCarritoResponse> items = carrito.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal subtotalTotal = items.stream()
                .map(ItemCarritoResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = subtotalTotal.add(tarifaEnvio);

        return new CarritoResponse(carrito.getId(), items, tarifaEnvio, total, carrito.getUltimaActualizacion());
    }

    private ItemCarritoResponse toItemResponse(ItemCarrito item) {
        Producto p = item.getProducto();
        CategoriaResponse cat = new CategoriaResponse(p.getCategoria().getId(), p.getCategoria().getNombre());
        ProductoResponse productoResponse = new ProductoResponse(
                p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.getImagenUrl(), p.isDisponible(), cat);
        return new ItemCarritoResponse(item.getId(), productoResponse, item.getCantidad(), item.getPrecioUnitario(), item.getSubtotal());
    }
}
