package com.grupocinco.mrelote.exception;

public class ProductoNoDisponibleException extends RuntimeException {
    public ProductoNoDisponibleException(String nombre) {
        super("El producto '" + nombre + "' no está disponible");
    }
}
