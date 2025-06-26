package com.example.studioapp_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "instructor_priority_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstructorPriorityRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "relative_priority", nullable = false)
    private Integer relativePriority = 1; // Default in Java, matches DB default

    @Column(name = "block_length_hours", nullable = false)
    private Integer blockLengthHours; // DB CHECK (block_length_hours > 0)

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // Default in Java, matches DB default

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // --- Relationships ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_location_id") // Nullable
    private StudioLocation studioLocation;

    @PrePersist
    @PreUpdate
    private void validateRequest() {
        if (blockLengthHours == null || blockLengthHours <= 0) {
            throw new IllegalArgumentException("Block length in hours must be positive.");
        }
        if (relativePriority == null || relativePriority <=0) {
            // Or handle default if null, but DB sets default 1
            // For Java validation, ensure it's positive if provided.
            // Or rely on DB default if not set in DTO.
            // Let's assume it's required for now from DTO.
            throw new IllegalArgumentException("Relative priority must be positive.");
        }
    }
}