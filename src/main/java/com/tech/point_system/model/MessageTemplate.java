package com.tech.point_system.model;

import com.tech.point_system._enum.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "message_templates",
        indexes = {
                @Index(name = "idx_msg_template_company", columnList = "company_id"),
                @Index(name = "idx_msg_template_company_type", columnList = "company_id, type, isEnabled")
        }
)
public class MessageTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Boolean isEnabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageTemplate other)) return false;
        return this.id != null && this.id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}