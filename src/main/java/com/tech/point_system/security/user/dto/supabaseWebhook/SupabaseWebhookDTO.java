package com.tech.point_system.security.user.dto.supabaseWebhook;

public record SupabaseWebhookDTO(
        String type,
        String table,
        SupabaseUserRecord record
) {}

