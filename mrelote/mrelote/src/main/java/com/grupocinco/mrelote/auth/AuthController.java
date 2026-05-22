package com.grupocinco.mrelote.auth;

import com.grupocinco.mrelote.auth.dto.LoginRequest;
import com.grupocinco.mrelote.auth.dto.LoginResult;
import com.grupocinco.mrelote.auth.dto.RegisterRequest;
import com.grupocinco.mrelote.auth.dto.UserProfileResponse;
import com.grupocinco.mrelote.auth.supabase.dto.SupabaseTokens;
import com.grupocinco.mrelote.domain.usuario.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService cookies;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<UserProfileResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);
        return tokensResponse(result.tokens()).body(result.usuario());
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request) {
        String refresh = cookies.readRefresh(request)
                .orElseThrow(() -> new BadCredentialsException("Refresh token ausente"));
        SupabaseTokens tokens;
        try {
            tokens = authService.refresh(refresh);
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, cookies.clearAccessCookie())
                    .header(HttpHeaders.SET_COOKIE, cookies.clearRefreshCookie())
                    .build();
        }
        return tokensResponse(tokens).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        cookies.readAccess(request).ifPresent(authService::logout);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clearAccessCookie())
                .header(HttpHeaders.SET_COOKIE, cookies.clearRefreshCookie())
                .build();
    }

    @GetMapping("/me")
    public UserProfileResponse me(@CurrentUser Usuario usuario) {
        return authService.profileBySupabaseId(usuario.getSupabaseUserId());
    }

    private ResponseEntity.BodyBuilder tokensResponse(SupabaseTokens tokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.accessCookie(tokens.accessToken(), tokens.expiresIn()))
                .header(HttpHeaders.SET_COOKIE, cookies.refreshCookie(tokens.refreshToken()));
    }
}
