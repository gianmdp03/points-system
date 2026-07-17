package com.tech.point_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "points_transactions")
public class PointsTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "points_account_id", nullable = false)
    private PointsAccount account;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private String transactionType;

    private Long referenceId;

    @Column(nullable = false)
    private OffsetDateTime createdAt;
}