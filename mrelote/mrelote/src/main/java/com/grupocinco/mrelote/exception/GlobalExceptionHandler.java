package com.grupocinco.mrelote.exception;

import com.grupocinco.mrelote.auth.supabase.SupabaseAuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return new ApiError(400, "Bad Request", message, request.getRequestURI());
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDuplicateEmail(CorreoDuplicadoException ex, HttpServletRequest request) {
        return new ApiError(409, "Conflict", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleBadCredentials(HttpServletRequest request) {
        return new ApiError(401, "Unauthorized", "Correo o contraseña incorrectos", request.getRequestURI());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return new ApiError(404, "Not Found", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleAccesoDenegado(AccesoDenegadoException ex, HttpServletRequest request) {
        return new ApiError(403, "Forbidden", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ProductoNoDisponibleException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError handleProductoNoDisponible(ProductoNoDisponibleException ex, HttpServletRequest request) {
        return new ApiError(422, "Unprocessable Entity", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError handleReglaDeNegocio(ReglaDeNegocioException ex, HttpServletRequest request) {
        return new ApiError(422, "Unprocessable Entity", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(SupabaseAuthException.class)
    public ResponseEntity<ApiError> handleSupabaseAuth(SupabaseAuthException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus() >= 500 || ex.getStatus() == 0
                ? HttpStatus.BAD_GATEWAY
                : HttpStatus.valueOf(ex.getStatus());
        return ResponseEntity.status(status).body(new ApiError(
                status.value(),
                status.getReasonPhrase(),
                "Error en el proveedor de autenticación",
                request.getRequestURI()
        ));
    }
}
