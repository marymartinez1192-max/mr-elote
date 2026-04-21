package com.grupocinco.mrelote.admin;

import com.grupocinco.mrelote.admin.dto.ActualizarEstadoPedidoRequest;
import com.grupocinco.mrelote.domain.pedido.EstadoPedido;
import com.grupocinco.mrelote.domain.pedido.Pedido;
import com.grupocinco.mrelote.domain.pedido.PedidoRepository;
import com.grupocinco.mrelote.exception.RecursoNoEncontradoException;
import com.grupocinco.mrelote.exception.ReglaDeNegocioException;
import com.grupocinco.mrelote.pedido.dto.ItemPedidoResponse;
import com.grupocinco.mrelote.pedido.dto.PedidoPageResponse;
import com.grupocinco.mrelote.pedido.dto.PedidoResponse;
import com.grupocinco.mrelote.producto.dto.CategoriaResponse;
import com.grupocinco.mrelote.producto.dto.ProductoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminPedidoService {

    private final PedidoRepository pedidoRepository;

    // RN-05: transiciones válidas
    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            EstadoPedido.PENDIENTE, Set.of(EstadoPedido.ACEPTADO, EstadoPedido.CANCELADO),
            EstadoPedido.ACEPTADO,  Set.of(EstadoPedido.EN_CAMINO, EstadoPedido.CANCELADO),
            EstadoPedido.EN_CAMINO, Set.of(EstadoPedido.ENTREGADO)
    );

    public PedidoPageResponse listarTodos(EstadoPedido estado, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
        Page<Pedido> resultado = estado != null
                ? pedidoRepository.findByEstado(estado, pageable)
                : pedidoRepository.findAll(pageable);

        List<PedidoResponse> content = resultado.getContent().stream()
                .map(this::toResponse).toList();

        return new PedidoPageResponse(content, resultado.getTotalElements(),
                resultado.getTotalPages(), page, size);
    }

    @Transactional
    public PedidoResponse actualizarEstado(Long pedidoId, ActualizarEstadoPedidoRequest request) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido", pedidoId));

        EstadoPedido estadoActual = pedido.getEstado();
        EstadoPedido estadoNuevo = request.estado();

        Set<EstadoPedido> permitidos = TRANSICIONES.getOrDefault(estadoActual, Set.of());
        if (!permitidos.contains(estadoNuevo)) {
            throw new ReglaDeNegocioException(
                    "Transición inválida: " + estadoActual + " → " + estadoNuevo);
        }

        pedido.setEstado(estadoNuevo);
        return toResponse(pedidoRepository.save(pedido));
    }

    private PedidoResponse toResponse(Pedido pedido) {
        List<ItemPedidoResponse> items = pedido.getItems().stream()
                .map(item -> {
                    var p = item.getProducto();
                    CategoriaResponse cat = new CategoriaResponse(p.getCategoria().getId(), p.getCategoria().getNombre());
                    ProductoResponse prod = new ProductoResponse(p.getId(), p.getNombre(), p.getDescripcion(),
                            p.getPrecio(), p.getImagenUrl(), p.isDisponible(), cat);
                    return new ItemPedidoResponse(item.getId(), prod, item.getCantidad(),
                            item.getPrecioUnitario(), item.getSubtotal());
                }).toList();

        return new PedidoResponse(pedido.getId(), pedido.getEstado(), items,
                pedido.getTarifaEnvio(), pedido.getTotal(), pedido.getFechaCreacion());
    }
}
