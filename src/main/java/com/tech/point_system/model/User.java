package com.tech.point_system.model;

import com.tech.point_system._enum.Role;
import com.tech.point_system._enum.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_plan_expiration", columnList = "current_plan, plan_expiration_date"),
                @Index(name = "idx_user_free_trial", columnList = "role, isFreeTrialOver, freeTrialEndTime")
        }
)
public class User {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String dni;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    private Boolean isFreeTrialOver;

    private LocalDate freeTrialStartTime;

    private LocalDate freeTrialEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_plan", nullable = false)
    private SubscriptionPlan currentPlan = SubscriptionPlan.NONE;

    @Column(name = "plan_expiration_date")
    private OffsetDateTime planExpirationDate;

    @Builder
    public User(String id, String email, String name, String dni, Role role, SubscriptionPlan currentPlan, OffsetDateTime planExpirationDate) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.dni = dni;
        this.role = role;
        this.currentPlan = currentPlan != null ? currentPlan : SubscriptionPlan.NONE;
        this.planExpirationDate = planExpirationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return this.id != null && this.id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
