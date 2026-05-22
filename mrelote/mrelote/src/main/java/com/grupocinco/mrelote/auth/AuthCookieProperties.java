package com.grupocinco.mrelote.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.cookie")
public record AuthCookieProperties(
        String accessName,
        String refreshName,
        String domain,
        boolean secure,
        String sameSite
) {}
