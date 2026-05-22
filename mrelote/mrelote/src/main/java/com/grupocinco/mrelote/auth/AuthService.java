package com.grupocinco.mrelote.auth;

import com.grupocinco.mrelote.auth.dto.LoginRequest;
import com.grupocinco.mrelote.auth.dto.LoginResult;
import com.grupocinco.mrelote.auth.dto.RegisterRequest;
import com.grupocinco.mrelote.auth.dto.UserProfileResponse;
import com.grupocinco.mrelote.auth.supabase.SupabaseAuthClient;
import com.grupocinco.mrelote.auth.supabase.SupabaseAuthException;
import com.grupocinco.mrelote.auth.supabase.dto.SupabaseTokens;
import com.grupocinco.mrelote.auth.supabase.dto.SupabaseUser;
import com.grupocinco.mrelote.domain.usuario.Rol;
import com.grupocinco.mrelote.domain.usuario.Usuario;
import com.grupocinco.mrelote.domain.usuario.UsuarioRepository;
import com.grupocinco.mrelote.exception.CorreoDuplicadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final SupabaseAuthClient supabase;

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new CorreoDuplicadoException(request.correo());
        }

        SupabaseUser created;
        try {
            created = supabase.signUp(request.correo(), request.password(), request.redirectTo());
        } catch (SupabaseAuthException ex) {
            if (isDuplicateEmail(ex)) {
                throw new CorreoDuplicadoException(request.correo());
            }
            throw ex;
        }

        Usuario usuario = new Usuario();
        usuario.setSupabaseUserId(created.id());
        usuario.setNombre(request.nombre());
        usuario.setTelefono(request.telefono());
        usuario.setCorreo(request.correo());
        usuario.setDireccion(request.direccion());
        usuario.setRol(Rol.CLIENTE);
        usuarioRepository.save(usuario);

        return toProfile(usuario);
    }

    public LoginResult login(LoginRequest request) {
        SupabaseTokens tokens;
        try {
            tokens = supabase.signInWithPassword(request.correo(), request.password());
        } catch (SupabaseAuthException ex) {
            if (isInvalidCredentials(ex)) {
                throw new BadCredentialsException("Credenciales inválidas");
            }
            throw ex;
        }

        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        return new LoginResult(tokens, toProfile(usuario));
    }

    public SupabaseTokens refresh(String refreshToken) {
        try {
            return supabase.refresh(refreshToken);
        } catch (SupabaseAuthException ex) {
            throw new BadCredentialsException("Refresh token inválido");
        }
    }

    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            supabase.signOut(accessToken);
        } catch (SupabaseAuthException ignored) {
            // Best-effort logout.
        }
    }

    public UserProfileResponse profileBySupabaseId(UUID supabaseUserId) {
        Usuario usuario = usuarioRepository.findBySupabaseUserId(supabaseUserId)
                .orElseThrow(() -> new BadCredentialsException("Usuario no registrado"));
        return toProfile(usuario);
    }

    private boolean isDuplicateEmail(SupabaseAuthException ex) {
        if (ex.getStatus() == 422 || ex.getStatus() == 409 || ex.getStatus() == 400) {
            String code = ex.getErrorCode();
            return code != null && (
                    code.contains("already") ||
                    code.equalsIgnoreCase("email_exists") ||
                    code.equalsIgnoreCase("user_already_exists")
            );
        }
        return false;
    }

    private boolean isInvalidCredentials(SupabaseAuthException ex) {
        return ex.getStatus() == 400 || ex.getStatus() == 401;
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
