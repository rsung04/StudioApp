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
@Table(name = "instructor_class_qualifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstructorClassQualification {

    @EmbeddedId // Marks that the ID is an embeddable class
    private InstructorClassQualificationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("instructorId") // Maps the 'instructorId' field of the EmbeddedId
    @JoinColumn(name = "instructor_id")
    private Instructor instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("classDefinitionId") // Maps the 'classDefinitionId' field of the EmbeddedId
    @JoinColumn(name = "class_definition_id")
    private ClassDefinition classDefinition;

    @Lob
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // The extra column on the join table

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}