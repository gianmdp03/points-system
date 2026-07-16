package com.tech.point_system.security.user.service;

import com.tech.point_system.security.user.dto.supabaseWebhook.SupabaseWebhookDTO;

public interface WebhookService {
    void processUserWebhook(SupabaseWebhookDTO payload);
}
