package com.grupocinco.mrelote.admin;

import com.grupocinco.mrelote.admin.dto.ConfigNegocioRequest;
import com.grupocinco.mrelote.admin.dto.ConfigNegocioResponse;
import com.grupocinco.mrelote.config.ZonaNegocio;
import com.grupocinco.mrelote.domain.negocio.ConfigNegocio;
import com.grupocinco.mrelote.domain.negocio.ConfigNegocioRepository;
import com.grupocinco.mrelote.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class NegocioService {

    private final ConfigNegocioRepository configNegocioRepository;

    public ConfigNegocioResponse obtenerConfig() {
        return toResponse(getConfig());
    }

    public ConfigNegocioResponse actualizarConfig(ConfigNegocioRequest request) {
        ConfigNegocio config = getConfig();
        config.setHorarioApertura(request.horarioApertura());
        config.setHorarioCierre(request.horarioCierre());
        config.setCerradoManual(request.cerradoManual());
        return toResponse(configNegocioRepository.save(config));
    }

    public boolean estaAbierto() {
        ConfigNegocio config = getConfig();
        if (config.isCerradoManual()) {
            return false;
        }
        LocalTime ahora = ZonaNegocio.horaActual();
        return !ahora.isBefore(config.getHorarioApertura()) && !ahora.isAfter(config.getHorarioCierre());
    }

    public void validarNegocioAbierto() {
        if (!estaAbierto()) {
            throw new ReglaDeNegocioException("El negocio está cerrado y no acepta pedidos en este momento");
        }
    }

    private ConfigNegocio getConfig() {
        return configNegocioRepository.findFirstBy()
                .orElseThrow(() -> new IllegalStateException("Configuración del negocio no encontrada"));
    }

    private ConfigNegocioResponse toResponse(ConfigNegocio c) {
        return new ConfigNegocioResponse(c.getId(), c.getHorarioApertura(), c.getHorarioCierre(), c.isCerradoManual());
    }
}
