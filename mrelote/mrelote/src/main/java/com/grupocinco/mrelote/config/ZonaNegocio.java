package com.grupocinco.mrelote.config;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class ZonaNegocio {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private ZonaNegocio() {
    }

    public static LocalTime horaActual() {
        return LocalTime.now(ZONA);
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }
}
