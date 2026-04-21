package com.grupocinco.mrelote.auth.dto;

public record AuthResponse(
        String token,
        UserProfileResponse usuario
) {}
