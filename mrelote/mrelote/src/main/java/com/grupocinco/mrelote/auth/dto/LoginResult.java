package com.grupocinco.mrelote.auth.dto;

import com.grupocinco.mrelote.auth.supabase.dto.SupabaseTokens;

public record LoginResult(SupabaseTokens tokens, UserProfileResponse usuario) {}
