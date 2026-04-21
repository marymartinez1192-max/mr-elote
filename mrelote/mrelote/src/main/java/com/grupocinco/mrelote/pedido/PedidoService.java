package com.grupocinco.mrelote.pedido;

import com.grupocinco.mrelote.admin.NegocioService;
import com.grupocinco.mrelote.domain.carrito.Carrito;
import com.grupocinco.mrelote.domain.carrito.CarritoRepository;
import com.grupocinco.mrelote.domain.carrito.ItemCarrito;
import com.grupocinco.mrelote.domain.pedido.EstadoPedido;
import com.grupocinco.mrelote.domain.pedido.ItemPedido;
import com.grupocinco.mrelote.domain.pedido.Pedido;
import com.grupocinco.mrelote.domain.pedido.PedidoRepository;
import com.grupocinco.mrelote.domain.usuario.Usuario;
import com.grupocinco.mrelote.exception.AccesoDenegadoException;
import com.grupocinco.mrelote.exception.RecursoNoEncontradoException;
import com.grupocinco.mrelote.exception.ReglaDeNegocioException;
import com.grupocinco.mrelote.pedido.dto.ItemPedidoResponse;
import com.grupocinco.mrelote.pedido.dto.PedidoPageResponse;
import com.grupocinco.mrelote.pedido.dto.PedidoResponse;
import com.grupocinco.mrelote.producto.dto.CategoriaResponse;
import com.grupocinco.mrelote.producto.dto.ProductoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final CarritoRepository carritoRepository;
    private final NegocioService negocioService;

    @Value("${app.carrito.tarifa-envio:3000}")
    private BigDecimal tarifaEnvio;

    @Transactional
    public PedidoResponse confirmar(Usuario usuario) {
        // RN-06: el negocio debe estar abierto para aceptar pedidos
        negocioService.validarNegocioAbierto();

        Carrito carrito = carritoRepository.findByUsuario(usuario)
                .orElseThrow(() -> new ReglaDeNegocioException("El usuario no tiene un carrito activo"));

        if (carrito.getItems().isEmpty()) {
            throw new ReglaDeNegocioException("El carrito está vacío");
        }

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setTarifaEnvio(tarifaEnvio);

        BigDecimal subtotalTotal = BigDecimal.ZERO;
        for (ItemCarrito itemCarrito : carrito.getItems()) {
            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setPedido(pedido);
            itemPedido.setProducto(itemCarrito.getProducto());
            itemPedido.setCantidad(itemCarrito.getCantidad());
            itemPedido.setPrecioUnitario(itemCarrito.getPrecioUnitario());
            itemPedido.setSubtotal(itemCarrito.getSubtotal());
            pedido.getItems().add(itemPedido);
            subtotalTotal = subtotalTotal.add(itemCarrito.getSubtotal());
        }

        pedido.setTotal(subtotalTotal.add(tarifaEnvio));

        // RN-09: confirmar pedido vacía el carrito
        carrito.getItems().clear();
        carritoRepository.save(carrito);

        return toResponse(pedidoRepository.save(pedido));
    }

    public PedidoResponse obtener(Usuario usuario, Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido", pedidoId));

        validarPropietario(pedido, usuario);
        return toResponse(pedido);
    }

    public PedidoPageResponse listar(Usuario usuario, int page, int size) {
        Page<Pedido> resultado = pedidoRepository.findByUsuario(
                usuario, PageRequest.of(page, size, Sort.by("fechaCreacion").descending()));

        List<PedidoResponse> content = resultado.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PedidoPageResponse(content, resultado.getTotalElements(),
                resultado.getTotalPages(), page, size);
    }

    @Transactional
    public PedidoResponse cancelar(Usuario usuario, Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido", pedidoId));

        validarPropietario(pedido, usuario);

        // RN-04: solo se puede cancelar si está en PENDIENTE
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new ReglaDeNegocioException(
                    "El pedido no puede cancelarse porque está en estado " + pedido.getEstado());
        }

        pedido.setEstado(EstadoPedido.CANCELADO);
        return toResponse(pedidoRepository.save(pedido));
    }

    private void validarPropietario(Pedido pedido, Usuario usuario) {
        if (!pedido.getUsuario().getId().equals(usuario.getId())) {
            throw new AccesoDenegadoException();
        }
    }

    private PedidoResponse toResponse(Pedido pedido) {
        List<ItemPedidoResponse> items = pedido.getItems().stream()
                .map(this::toItemResponse)
                .toList();
        return new PedidoResponse(pedido.getId(), pedido.getEstado(), items,
                pedido.getTarifaEnvio(), pedido.getTotal(), pedido.getFechaCreacion());
    }

    private ItemPedidoResponse toItemResponse(ItemPedido item) {
        var p = item.getProducto();
        CategoriaResponse cat = new CategoriaResponse(p.getCategoria().getId(), p.getCategoria().getNombre());
        ProductoResponse prod = new ProductoResponse(p.getId(), p.getNombre(), p.getDescripcion(),
                p.getPrecio(), p.getImagenUrl(), p.isDisponible(), cat);
        return new ItemPedidoResponse(item.getId(), prod, item.getCantidad(),
                item.getPrecioUnitario(), item.getSubtotal());
    }
}
