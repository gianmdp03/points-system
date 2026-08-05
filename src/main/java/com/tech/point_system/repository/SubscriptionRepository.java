package com.tech.point_system.repository;

import com.tech.point_system.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserId(String userId);
    Optional<Subscription> findByExternalSubscriptionId(String externalSubscriptionId);
}