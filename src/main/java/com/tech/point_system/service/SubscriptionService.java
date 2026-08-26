package com.tech.point_system.service;

import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system.dto.subscription.SubscriptionDetailDTO;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.dto.subscription.SubscriptionUpgradeRequestDTO;

public interface SubscriptionService {
    SubscriptionResponseDTO subscribeCompanyAdmin(String userId, SubscriptionRequestDTO dto);
    SubscriptionResponseDTO upgradeSubscription(String userId, SubscriptionPlan newPlan);
    SubscriptionResponseDTO upgradeSubscription(String userId, SubscriptionUpgradeRequestDTO dto);
    SubscriptionDetailDTO getMySubscription(String userId);
}






