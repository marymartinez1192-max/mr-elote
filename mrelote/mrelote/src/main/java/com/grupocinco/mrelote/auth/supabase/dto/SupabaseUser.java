package com.grupocinco.mrelote.auth.supabase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseUser(UUID id, String email) {}
