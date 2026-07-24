package com.tech.point_system.event;

import com.tech.point_system.model.Company;
import com.tech.point_system.security.user.model.User;

public record RewardRedeemEvent(Integer costInPoints, Company company, User user) {}
