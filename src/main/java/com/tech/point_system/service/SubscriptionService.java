package com.tech.point_system.service;

import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system.dto.subscription.SubscriptionDetailDTO;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;

public interface SubscriptionService {
    SubscriptionResponseDTO subscribeCompanyAdmin(String userId, SubscriptionRequestDTO dto);
    SubscriptionDetailDTO changeSubscriptionPlan(String userId, SubscriptionPlan newPlan);
    SubscriptionDetailDTO upgradeSubscription(String userId, SubscriptionPlan newPlan);
    SubscriptionDetailDTO getMySubscription(String userId);
    void cancelSubscription(String userId);
}
