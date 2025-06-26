package com.example.studioapp_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList; // For rooms list
import java.util.List;    // For rooms list

@Entity
@Table(name = "studio_locations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_studio_locations_organization_name", columnNames = {"organization_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudioLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Lob
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

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

    // A StudioLocation can have many Rooms
    @OneToMany(
        mappedBy = "studioLocation", // This 'studioLocation' is the field name in the Room entity
        cascade = CascadeType.ALL,   // Operations (persist, remove, etc.) on StudioLocation cascade to its Rooms
        orphanRemoval = true         // If a Room is removed from this list, it's deleted from DB
    )
    private List<Room> rooms = new ArrayList<>(); // Initialize to avoid nulls

    // Helper methods for managing the bidirectional relationship with Room (optional but good practice)
    public void addRoom(Room room) {
        rooms.add(room);
        room.setStudioLocation(this);
    }

    public void removeRoom(Room room) {
        rooms.remove(room);
        room.setStudioLocation(null);
    }
}