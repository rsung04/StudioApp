package com.example.studioapp_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime; // Use OffsetDateTime for TIMESTAMP WITH TIME ZONE

@Entity
@Table(name = "organizations") // Specifies the database table name
@Getter // Lombok:  generates all getter methods
@Setter // Lombok:  generates all setter methods
@NoArgsConstructor // Lombok: generates a no-argument constructor
@AllArgsConstructor // Lombok: generates a constructor with all arguments
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Assumes your 'id' column is auto-incrementing (like SERIAL or IDENTITY)
    private Integer id; // Matches 'integer NOT NULL'

    @Column(name = "name", nullable = false, unique = true, length = 255) // Matches 'character varying(255) NOT NULL' and UNIQUE constraint
    private String name;

    @Column(name = "subdomain", unique = true, length = 100) // Matches 'character varying(100)' and UNIQUE constraint
    private String subdomain;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @CreationTimestamp // Hibernate will automatically set this on creation
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Matches 'timestamp with time zone'

    @UpdateTimestamp // Hibernate will automatically set this on update
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt; // Matches 'timestamp with time zone'

    // --- Relationships to other tables will be added later ---
    // For example, an Organization might have many StudioLocations, Instructors, etc.
    // @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<StudioLocation> studioLocations;

    // @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Instructor> instructors;

}