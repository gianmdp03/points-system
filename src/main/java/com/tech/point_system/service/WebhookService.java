package com.tech.point_system.service;

import com.tech.point_system.dto.supabaseWebhook.SupabaseWebhookDTO;

public interface WebhookService {
    void processUserWebhook(SupabaseWebhookDTO payload);
}
