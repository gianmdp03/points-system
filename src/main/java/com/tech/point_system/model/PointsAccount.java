package com.tech.point_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "points_accounts",
        uniqueConstraints = {@UniqueConstraint(name = "uk_points_account_client_company", columnNames = {"client_id", "company_id"})},
        indexes = {
                @Index(name = "idx_points_account_company_activity", columnList = "company_id, lastActivityDate"),
                @Index(name = "idx_points_account_retention", columnList = "company_id, lastActivityDate, lastRetentionNotificationDate"),
                @Index(name = "idx_points_account_client_id", columnList = "client_id")
        }
)
public class PointsAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(nullable = false)
  private Integer balance = 0;

  private OffsetDateTime lastActivityDate;

  private OffsetDateTime lastRetentionNotificationDate;

  @OneToMany(mappedBy = "pointsAccount", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<PointsTransaction> transactions = new HashSet<>();

  @Version
  private Long version;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PointsAccount other)) return false;

    return this.id != null && this.id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
