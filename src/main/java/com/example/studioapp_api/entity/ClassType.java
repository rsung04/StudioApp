package com.example.studioapp_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
// import java.util.List; // If ClassType has a OneToMany to ClassDefinition

@Entity
@Table(name = "class_types", uniqueConstraints = {
    @UniqueConstraint(name = "uq_class_types_organization_name", columnNames = {"organization_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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

    // If a ClassType can have many ClassDefinitions (likely)
    // @OneToMany(mappedBy = "classType", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<ClassDefinition> classDefinitions = new ArrayList<>();
}