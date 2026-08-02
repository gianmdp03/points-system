package com.tech.point_system.model;

import com.tech.point_system._enum.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
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

    private OffsetDateTime freeTrialStartTime;

    @Builder
    public User(String id, String email, String name, String dni, Role role) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.dni = dni;
        this.role = role;
    }
}
