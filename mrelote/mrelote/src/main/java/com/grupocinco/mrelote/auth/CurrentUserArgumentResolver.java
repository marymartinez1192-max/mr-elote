package com.grupocinco.mrelote.auth;

import com.grupocinco.mrelote.domain.usuario.Usuario;
import com.grupocinco.mrelote.domain.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UsuarioRepository usuarioRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && Usuario.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new BadCredentialsException("Sesión no válida");
        }
        UUID supabaseUserId;
        try {
            supabaseUserId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new BadCredentialsException("Sesión no válida");
        }
        return usuarioRepository.findBySupabaseUserId(supabaseUserId)
                .orElseThrow(() -> new BadCredentialsException("Usuario no registrado"));
    }
}
