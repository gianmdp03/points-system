package com.tech.point_system.dto.supabaseWebhook;

public record SupabaseWebhookDTO(
        String type,
        String table,
        SupabaseUserRecord record
) {}

