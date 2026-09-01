package com.tech.point_system.service;

import com.tech.point_system._enum.NotificationType;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;

import java.util.Map;

public interface EmailService {
    void sendNotificationEmail(
            NotificationType type, Company company, Client client, Map<String, Object> extraParams);
}


