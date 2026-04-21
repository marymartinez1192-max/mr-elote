package com.grupocinco.mrelote.exception;

public class CorreoDuplicadoException extends RuntimeException {
    public CorreoDuplicadoException(String correo) {
        super("El correo ya está registrado: " + correo);
    }
}
