package com.grupocinco.mrelote.domain.negocio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "config_negocio")
@Getter
@Setter
@NoArgsConstructor
public class ConfigNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalTime horarioApertura;

    @Column(nullable = false)
    private LocalTime horarioCierre;

    @Column(nullable = false)
    private boolean cerradoManual = false;
}
