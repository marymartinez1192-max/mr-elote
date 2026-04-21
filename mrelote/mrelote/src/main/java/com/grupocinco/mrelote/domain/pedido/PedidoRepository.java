package com.grupocinco.mrelote.domain.pedido;

import com.grupocinco.mrelote.domain.usuario.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Page<Pedido> findByUsuario(Usuario usuario, Pageable pageable);
    Page<Pedido> findByEstado(EstadoPedido estado, Pageable pageable);
}
