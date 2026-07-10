package com.tech.point_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    //private User admin;

    @OneToMany(mappedBy = "company")
    private Set<PointsAccount> pointsAccount = new HashSet<>();

    @OneToMany(mappedBy = "company")
    private Set<Product> product = new HashSet<>();

    @OneToMany(mappedBy = "company")
    private Set<Promotion> promotion = new HashSet<>();

    @OneToMany(mappedBy = "company")
    private Set<Reward> reward = new HashSet<>();
}
