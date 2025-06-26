package com.example.studioapp_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.Objects;

@Embeddable // Marks this class as embeddable, suitable for composite keys
@Data       // Lombok for boilerplate
@NoArgsConstructor
@AllArgsConstructor
public class InstructorClassQualificationId implements Serializable {
    // Must implement Serializable for composite keys

    @Column(name = "instructor_id")
    private Integer instructorId;

    @Column(name = "class_definition_id")
    private Integer classDefinitionId;

    // equals() and hashCode() are crucial for composite keys
    // Lombok's @Data should generate them, but it's good practice to be aware
    // and sometimes override if specific logic is needed (not usually for simple IDs).
    // If not using @Data or if it doesn't work as expected for composite keys:
    /*
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstructorClassQualificationId that = (InstructorClassQualificationId) o;
        return Objects.equals(instructorId, that.instructorId) &&
               Objects.equals(classDefinitionId, that.classDefinitionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instructorId, classDefinitionId);
    }
    */
}