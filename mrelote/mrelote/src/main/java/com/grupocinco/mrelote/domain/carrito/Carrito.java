package com.grupocinco.mrelote.domain.carrito;

import com.grupocinco.mrelote.config.ZonaNegocio;
import com.grupocinco.mrelote.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carritos")
@Getter
@Setter
@NoArgsConstructor
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCarrito> items = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = ZonaNegocio.ahora();

    @Column(nullable = false)
    private LocalDateTime ultimaActualizacion = ZonaNegocio.ahora();
}
