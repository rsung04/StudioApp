package com.example.studioapp_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList; // UNCOMMENT
import java.util.List;    // UNCOMMENT

@Entity
@Table(name = "rooms", uniqueConstraints = {
    @UniqueConstraint(name = "uq_rooms_location_name", columnNames = {"studio_location_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    // ... (id, name, capacity, createdAt, updatedAt, studioLocation fields remain the same) ...
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "capacity")
    private Integer capacity; 

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_location_id", nullable = false)
    private StudioLocation studioLocation; 


    // UNCOMMENT THIS SECTION
    @OneToMany(
        mappedBy = "room", 
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<RoomOperatingHours> operatingHours = new ArrayList<>();

    // UNCOMMENT HELPER METHODS
    public void addOperatingHours(RoomOperatingHours hours) {
        operatingHours.add(hours);
        hours.setRoom(this);
    }

    public void removeOperatingHours(RoomOperatingHours hours) {
        operatingHours.remove(hours);
        hours.setRoom(null);
    }
}