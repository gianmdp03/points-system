package com.tech.point_system.repository;

import com.tech.point_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByDni(String dni);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.isFreeTrialOver = true " +
            "WHERE (u.isFreeTrialOver = false OR u.isFreeTrialOver IS NULL) " +
            "AND u.freeTrialStartTime IS NOT NULL " +
            "AND u.freeTrialStartTime < :thresholdDate")
    int disableExpiredFreeTrials(@Param("thresholdDate") OffsetDateTime thresholdDate);
}