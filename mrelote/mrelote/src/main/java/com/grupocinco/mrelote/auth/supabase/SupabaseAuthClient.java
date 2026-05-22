package com.grupocinco.mrelote.auth.supabase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupocinco.mrelote.auth.supabase.dto.SupabaseTokens;
import com.grupocinco.mrelote.auth.supabase.dto.SupabaseUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Component
public class SupabaseAuthClient {

    private final RestClient client;
    private final SupabaseProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public SupabaseAuthClient(SupabaseProperties props, RestClient.Builder builder) {
        this.props = props;
        this.client = builder
                .baseUrl(props.url() + "/auth/v1")
                .defaultHeader("apikey", props.anonKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public SupabaseUser adminCreateUser(String email, String password) {
        return exchange(() -> client.post()
                .uri("/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.serviceRoleKey())
                .body(Map.of(
                        "email", email,
                        "password", password,
                        "email_confirm", true
                ))
                .retrieve()
                .body(SupabaseUser.class));
    }

    public SupabaseUser signUp(String email, String password, String redirectTo) {
        return exchange(() -> client.post()
                .uri(uri -> {
                    var b = uri.path("/signup");
                    if (redirectTo != null && !redirectTo.isBlank()) {
                        b.queryParam("redirect_to", redirectTo);
                    }
                    return b.build();
                })
                .body(Map.of("email", email, "password", password))
                .retrieve()
                .body(SupabaseUser.class));
    }

    public Optional<SupabaseUser> adminFindUserByEmail(String email) {
        try {
            AdminUsersPage page = client.get()
                    .uri(uri -> uri.path("/admin/users").queryParam("email", email).build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.serviceRoleKey())
                    .retrieve()
                    .body(AdminUsersPage.class);
            if (page == null || page.users == null || page.users.isEmpty()) {
                return Optional.empty();
            }
            return page.users.stream()
                    .filter(u -> email.equalsIgnoreCase(u.email()))
                    .findFirst();
        } catch (HttpStatusCodeException ex) {
            throw mapError(ex);
        }
    }

    public SupabaseTokens signInWithPassword(String email, String password) {
        return exchange(() -> client.post()
                .uri(uri -> uri.path("/token").queryParam("grant_type", "password").build())
                .body(Map.of("email", email, "password", password))
                .retrieve()
                .body(SupabaseTokens.class));
    }

    public SupabaseTokens refresh(String refreshToken) {
        return exchange(() -> client.post()
                .uri(uri -> uri.path("/token").queryParam("grant_type", "refresh_token").build())
                .body(Map.of("refresh_token", refreshToken))
                .retrieve()
                .body(SupabaseTokens.class));
    }

    public void signOut(String accessToken) {
        try {
            client.post()
                    .uri("/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException ex) {
            // Best-effort: 401/404 are acceptable on logout (token already invalid).
            if (ex.getStatusCode().is5xxServerError()) {
                throw mapError(ex);
            }
        }
    }

    private <T> T exchange(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpStatusCodeException ex) {
            throw mapError(ex);
        }
    }

    private SupabaseAuthException mapError(HttpStatusCodeException ex) {
        String code = null;
        String message = ex.getMessage();
        try {
            SupabaseError err = mapper.readValue(ex.getResponseBodyAsByteArray(), SupabaseError.class);
            code = err.errorCode != null ? err.errorCode : err.error;
            if (err.message != null) message = err.message;
            else if (err.errorDescription != null) message = err.errorDescription;
            else if (err.msg != null) message = err.msg;
        } catch (Exception ignored) {
            // body not JSON — keep raw message
        }
        return new SupabaseAuthException(ex.getStatusCode().value(), code, message);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdminUsersPage(java.util.List<SupabaseUser> users) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SupabaseError {
        public String error;
        public String message;
        public String msg;
        @com.fasterxml.jackson.annotation.JsonProperty("error_code")
        public String errorCode;
        @com.fasterxml.jackson.annotation.JsonProperty("error_description")
        public String errorDescription;
    }
}
