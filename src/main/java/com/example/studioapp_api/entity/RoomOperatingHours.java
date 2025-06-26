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

// The 'enum DayOfWeekEnum { ... }' definition that was here previously has been REMOVED.
// We are now relying on the separate DayOfWeekEnum.java file.

@Entity
@Table(name = "room_operating_hours", uniqueConstraints = {
    @UniqueConstraint(name = "uq_room_operating_hours_day", columnNames = {"room_id", "day_of_week"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomOperatingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING) // Store enum as string in DB (MONDAY, TUESDAY, etc.)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeekEnum dayOfWeek; // This now correctly refers to your separate DayOfWeekEnum.java

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // For PostgreSQL TIME type

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;   // For PostgreSQL TIME type

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // --- Relationships ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room; // Lombok will generate setRoom(Room room) for this

    // PrePersist and PreUpdate can be used to validate endTime > startTime if not handled by DB check
    @PrePersist
    @PreUpdate
    private void validateTimes() {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            // You might want to use a more specific exception type for validation errors
            throw new IllegalStateException("End time must be after start time for room operating hours. Room ID: " + (room != null ? room.getId() : "N/A") + ", Day: " + dayOfWeek);
        }
    }
}