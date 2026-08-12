package com.tech.point_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "points_accounts",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"client_id", "company_id"})})
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
