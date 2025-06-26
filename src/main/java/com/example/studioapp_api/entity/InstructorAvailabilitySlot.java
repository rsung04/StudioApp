package com.example.studioapp_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "instructor_availability_slots") // No table-level unique constraints beyond PK in schema for this
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstructorAvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeekEnum dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // Maps to TIME WITHOUT TIME ZONE

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;   // Maps to TIME WITHOUT TIME ZONE

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
    @JoinColumn(name = "studio_location_id") // Nullable
    private StudioLocation studioLocation; // Can be null if availability is org-wide

    @PrePersist
    @PreUpdate
    private void validateTimes() {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalStateException("End time must be after start time for availability slot. Instructor ID: " + 
                (instructor != null ? instructor.getId() : "N/A") + ", Day: " + dayOfWeek);
        }
    }
}