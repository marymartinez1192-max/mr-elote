package com.grupocinco.mrelote.config;

import com.grupocinco.mrelote.domain.negocio.ConfigNegocio;
import com.grupocinco.mrelote.domain.negocio.ConfigNegocioRepository;
import com.grupocinco.mrelote.domain.usuario.Rol;
import com.grupocinco.mrelote.domain.usuario.Usuario;
import com.grupocinco.mrelote.domain.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfigNegocioRepository configNegocioRepository;

    @Value("${app.admin.nombre}")
    private String nombre;

    @Value("${app.admin.telefono}")
    private String telefono;

    @Value("${app.admin.correo}")
    private String correo;

    @Value("${app.admin.password}")
    private String password;

    @Value("${app.admin.direccion}")
    private String direccion;

    @Override
    public void run(ApplicationArguments args) {
        if (!usuarioRepository.existsByCorreo(correo)) {
            Usuario admin = new Usuario();
            admin.setNombre(nombre);
            admin.setTelefono(telefono);
            admin.setCorreo(correo);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setDireccion(direccion);
            admin.setRol(Rol.ADMIN);
            usuarioRepository.save(admin);
        }

        if (configNegocioRepository.findFirstBy().isEmpty()) {
            ConfigNegocio config = new ConfigNegocio();
            config.setHorarioApertura(LocalTime.of(8, 0));
            config.setHorarioCierre(LocalTime.of(20, 0));
            config.setCerradoManual(false);
            configNegocioRepository.save(config);
        }
    }
}
