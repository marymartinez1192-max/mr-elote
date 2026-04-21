package com.grupocinco.mrelote.auth.dto;

import com.grupocinco.mrelote.domain.usuario.Rol;

public record UserProfileResponse(
        Long id,
        String nombre,
        String telefono,
        String correo,
        String direccion,
        Rol rol
) {}
