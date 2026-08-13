package com.tech.point_system.dto.supabaseWebhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SupabaseWebhookDTO(
        String type,
        String table,
        SupabaseUserRecord record,
        @JsonProperty("old_record")
        SupabaseUserRecord oldRecord
) {}