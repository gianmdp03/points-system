package com.tech.point_system.repository;

import com.tech.point_system.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findTopByUserIdOrderByIdDesc(String userId);

    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId ORDER BY s.id DESC LIMIT 1")
    Optional<Subscription> findByUserId(@Param("userId") String userId);

    Optional<Subscription> findByExternalSubscriptionId(String externalSubscriptionId);
}
