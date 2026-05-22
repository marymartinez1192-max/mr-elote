package com.grupocinco.mrelote.config;

import com.grupocinco.mrelote.auth.AuthCookieProperties;
import com.grupocinco.mrelote.auth.CookieBearerTokenResolver;
import com.grupocinco.mrelote.auth.CorsProperties;
import com.grupocinco.mrelote.auth.OriginCheckFilter;
import com.grupocinco.mrelote.domain.usuario.Usuario;
import com.grupocinco.mrelote.domain.usuario.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthCookieProperties cookieProps,
                                           CorsProperties corsProps,
                                           UsuarioRepository usuarioRepository) {
        CookieBearerTokenResolver cookieResolver = new CookieBearerTokenResolver(cookieProps.accessName());
        OriginCheckFilter originFilter = new OriginCheckFilter(corsProps.allowedOrigins());

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/register", "/auth/refresh", "/auth/logout").permitAll()
                        .requestMatchers("/categories", "/products", "/business/status").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(originFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2ResourceServer(rs -> rs
                        .bearerTokenResolver(cookieResolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(usuarioRepository)))
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProps) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(corsProps.allowedOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Content-Type", "Accept"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter(UsuarioRepository repo) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> authoritiesFor(jwt, repo));
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    private Collection<GrantedAuthority> authoritiesFor(Jwt jwt, UsuarioRepository repo) {
        String sub = jwt.getSubject();
        if (sub == null) {
            return List.of();
        }
        UUID supabaseUserId;
        try {
            supabaseUserId = UUID.fromString(sub);
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
        Optional<Usuario> usuario = repo.findBySupabaseUserId(supabaseUserId);
        return usuario
                .map(u -> List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name())))
                .orElse(List.of());
    }
}
