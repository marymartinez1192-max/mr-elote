package com.grupocinco.mrelote.auth;

import com.grupocinco.mrelote.auth.dto.AuthResponse;
import com.grupocinco.mrelote.auth.dto.LoginRequest;
import com.grupocinco.mrelote.auth.dto.RegisterRequest;
import com.grupocinco.mrelote.auth.dto.UserProfileResponse;
import com.grupocinco.mrelote.domain.usuario.Rol;
import com.grupocinco.mrelote.domain.usuario.Usuario;
import com.grupocinco.mrelote.domain.usuario.UsuarioRepository;
import com.grupocinco.mrelote.exception.CorreoDuplicadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserProfileResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new CorreoDuplicadoException(request.correo());
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setTelefono(request.telefono());
        usuario.setCorreo(request.correo());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setDireccion(request.direccion());
        usuario.setRol(Rol.CLIENTE);

        usuarioRepository.save(usuario);

        return toProfile(usuario);
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        return new AuthResponse(jwtService.generateToken(usuario), toProfile(usuario));
    }

    private UserProfileResponse toProfile(Usuario u) {
        return new UserProfileResponse(
                u.getId(),
                u.getNombre(),
                u.getTelefono(),
                u.getCorreo(),
                u.getDireccion(),
                u.getRol()
        );
    }
}
