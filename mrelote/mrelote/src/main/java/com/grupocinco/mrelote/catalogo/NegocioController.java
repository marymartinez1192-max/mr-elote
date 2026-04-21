package com.grupocinco.mrelote.catalogo;

import com.grupocinco.mrelote.admin.NegocioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/business")
@RequiredArgsConstructor
public class NegocioController {

    private final NegocioService negocioService;

    @GetMapping("/status")
    public Map<String, Boolean> estado() {
        return Map.of("abierto", negocioService.estaAbierto());
    }
}
