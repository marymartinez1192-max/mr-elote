package com.grupocinco.mrelote.config;

import com.grupocinco.mrelote.auth.supabase.SupabaseAuthClient;
import com.grupocinco.mrelote.auth.supabase.SupabaseAuthException;
import com.grupocinco.mrelote.auth.supabase.dto.SupabaseUser;
import com.grupocinco.mrelote.domain.negocio.ConfigNegocio;
import com.grupocinco.mrelote.domain.negocio.ConfigNegocioRepository;
import com.grupocinco.mrelote.domain.usuario.Rol;
import com.grupocinco.mrelote.domain.usuario.Usuario;
import com.grupocinco.mrelote.domain.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final ConfigNegocioRepository configNegocioRepository;
    private final SupabaseAuthClient supabase;

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
        seedAdmin();
        seedConfigNegocio();
    }

    @Transactional
    void seedAdmin() {
        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            return;
        }

        Optional<SupabaseUser> existing;
        try {
            existing = supabase.adminFindUserByEmail(correo);
        } catch (SupabaseAuthException ex) {
            log.warn("No se pudo consultar el admin en Supabase ({}). Se omite el seeding.", ex.getMessage());
            return;
        }

        SupabaseUser supabaseUser;
        if (existing.isPresent()) {
            supabaseUser = existing.get();
        } else {
            try {
                supabaseUser = supabase.adminCreateUser(correo, password);
            } catch (SupabaseAuthException ex) {
                log.warn("No se pudo crear el admin en Supabase ({}). Se omite el seeding.", ex.getMessage());
                return;
            }
        }

        Usuario admin = new Usuario();
        admin.setSupabaseUserId(supabaseUser.id());
        admin.setNombre(nombre);
        admin.setTelefono(telefono);
        admin.setCorreo(correo);
        admin.setDireccion(direccion);
        admin.setRol(Rol.ADMIN);
        usuarioRepository.save(admin);
    }

    void seedConfigNegocio() {
        if (configNegocioRepository.findFirstBy().isEmpty()) {
            ConfigNegocio config = new ConfigNegocio();
            config.setHorarioApertura(LocalTime.of(8, 0));
            config.setHorarioCierre(LocalTime.of(20, 0));
            config.setCerradoManual(false);
            configNegocioRepository.save(config);
        }
    }
}
