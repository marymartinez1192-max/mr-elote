package com.grupocinco.mrelote.auth;

import com.grupocinco.mrelote.auth.dto.AuthResponse;
import com.grupocinco.mrelote.auth.dto.LoginRequest;
import com.grupocinco.mrelote.auth.dto.RegisterRequest;
import com.grupocinco.mrelote.auth.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
