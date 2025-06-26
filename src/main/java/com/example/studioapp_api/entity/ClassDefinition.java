package com.example.studioapp_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
// import java.util.Set; // For many-to-many with instructors if done from this side

@Entity
@Table(name = "class_definitions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_class_definitions_organization_code", columnNames = {"organization_id", "class_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "class_code", nullable = false, length = 100)
    private String classCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes; // CHECK constraint (duration_minutes > 0) handled by DB

    @Column(name = "level", length = 50)
    private String level; // Optional

    @Column(name = "default_studio_capacity_needed")
    private Integer defaultStudioCapacityNeeded; // Optional, CHECK constraint handled by DB

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
    @JoinColumn(name = "class_type_id", nullable = false)
    private ClassType classType;

    // Relationship to InstructorClassQualifications (Many-to-Many with Instructor)
    // This side might not be strictly necessary if you always manage qualifications
    // from the Instructor side or through a dedicated service for the join table.
    // If added, it would typically be:
    // @OneToMany(mappedBy = "classDefinition")
    // private Set<InstructorClassQualification> instructorQualifications = new HashSet<>();
}