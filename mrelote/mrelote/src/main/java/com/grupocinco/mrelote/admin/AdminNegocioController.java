package com.grupocinco.mrelote.admin;

import com.grupocinco.mrelote.admin.dto.ConfigNegocioRequest;
import com.grupocinco.mrelote.admin.dto.ConfigNegocioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/business-config")
@RequiredArgsConstructor
public class AdminNegocioController {

    private final NegocioService negocioService;

    @GetMapping
    public ConfigNegocioResponse obtener() {
        return negocioService.obtenerConfig();
    }

    @PutMapping
    public ConfigNegocioResponse actualizar(@Valid @RequestBody ConfigNegocioRequest request) {
        return negocioService.actualizarConfig(request);
    }
}
