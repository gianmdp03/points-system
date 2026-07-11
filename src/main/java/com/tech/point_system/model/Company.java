package com.tech.point_system.model;

import com.tech.point_system.extra.CompanyDetails;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JdbcTypeCode(SqlTypes.JSON)
    private CompanyDetails companyDetails;
    //private User admin;

    @OneToMany(mappedBy = "company")
    private Set<PointsAccount> pointsAccounts = new HashSet<>();

    @OneToMany(mappedBy = "company")
    private Set<Product> products = new HashSet<>();

    @OneToMany(mappedBy = "company")
    private Set<Promotion> promotions = new HashSet<>();

    @OneToMany(mappedBy = "company")
    private Set<Reward> rewards = new HashSet<>();
}
