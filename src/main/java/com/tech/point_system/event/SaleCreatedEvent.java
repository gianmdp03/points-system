package com.tech.point_system.event;

import com.tech.point_system.model.Company;
import com.tech.point_system.model.User;

import java.math.BigDecimal;

public record SaleCreatedEvent(BigDecimal amount, Company company, User user) {}
