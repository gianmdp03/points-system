package com.tech.point_system.model;

import com.tech.point_system.extra.CompanyDetails;
import com.tech.point_system.security.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    @OneToMany(mappedBy = "company")
    private PointsAccount pointsAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User admin;

    @Column(nullable = false)
    private boolean isEnabled = true;  //PONER TASK CON UN METODO QUE BUSCA COMPANIES MARCADAS PARA ELIMINACION 30 DIAS DESPUES.

    private LocalDate disabledDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountStep = new BigDecimal("100");

    @Column(nullable = false)
    private Integer pointsPerStep = 1;

    @OneToMany(mappedBy = "company")
    private Set<PointsAccount> pointsAccounts = new HashSet<>();

    @OneToMany(mappedBy = "company")
    private Set<Product> products = new HashSet<>();

    @OneToMany(mappedBy = "company")
    private Set<Promotion> promotions = new HashSet<>();

    @OneToMany(mappedBy = "company")
    private Set<Reward> rewards = new HashSet<>();

    @OneToMany(mappedBy = "company")
    private Set<Sale> sales = new HashSet<>();
}
