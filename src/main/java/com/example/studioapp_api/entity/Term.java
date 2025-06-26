package com.example.studioapp_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate; // For DATE type
import java.time.OffsetDateTime;

@Entity
@Table(name = "terms", uniqueConstraints = {
    // This constraint handles cases where studioLocation is NOT NULL
    @UniqueConstraint(name = "uq_terms_org_location_name", columnNames = {"organization_id", "studio_location_id", "name"}),
    // For cases where studioLocation IS NULL, PostgreSQL allows a unique constraint on (organization_id, name)
    // JPA doesn't have a direct way to model conditional unique constraints like "UNIQUE ... WHERE studio_location_id IS NULL"
    // The uq_terms_org_name_null_location constraint in your DB handles this.
    // We rely on the database to enforce the null-specific uniqueness.
    // Hibernate might complain during schema validation if it tries to infer the second one directly without a WHERE clause.
    // If schema validation with `ddl-auto=validate` becomes problematic due to this specific null-conditional unique constraint,
    // we might need to adjust how JPA sees it or set `ddl-auto` to `update` or `none` and fully rely on your DB schema.
    // For now, let's proceed and see.
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // Maps to SQL DATE

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;   // Maps to SQL DATE

    @Column(name = "is_active_for_planning", nullable = false)
    private boolean isActiveForPlanning = false; // Matches DEFAULT false

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // --- Relationships ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_location_id") // Nullable, as per your schema
    private StudioLocation studioLocation; // Can be null

    // Business logic validation (can be done in service layer or here)
    @PrePersist
    @PreUpdate
    private void validateDates() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalStateException("Term end date cannot be before start date.");
        }
    }
}