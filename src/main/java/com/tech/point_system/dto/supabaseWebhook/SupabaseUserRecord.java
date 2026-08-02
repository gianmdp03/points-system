package com.tech.point_system.dto.supabaseWebhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record SupabaseUserRecord(
        String id,
        String email,
        @JsonProperty("raw_user_meta_data")
        Map<String, Object> rawUserMetaData
) {}
