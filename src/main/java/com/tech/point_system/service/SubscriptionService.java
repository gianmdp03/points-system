package com.tech.point_system.service;

import com.tech.point_system.dto.subscription.SubscriptionDetailDTO;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;

public interface SubscriptionService {
    SubscriptionResponseDTO subscribeCompanyAdmin(String userId, SubscriptionRequestDTO dto);
    SubscriptionDetailDTO getMySubscription(String userId);
    void cancelSubscription(String userId);
}