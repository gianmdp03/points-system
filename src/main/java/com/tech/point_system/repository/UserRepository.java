package com.tech.point_system.repository;

import com.tech.point_system.model.User;
import com.tech.point_system._enum.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByDni(String dni);

    Optional<User> findByEmail(String email);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.isFreeTrialOver = true " +
            "WHERE u.role = :adminRole " +
            "AND u.isFreeTrialOver = false " +
            "AND u.freeTrialEndTime IS NOT NULL " +
            "AND u.freeTrialEndTime < :today")
    int disableExpiredFreeTrials(
            @Param("today") LocalDate today,
            @Param("adminRole") Role adminRole
    );
}