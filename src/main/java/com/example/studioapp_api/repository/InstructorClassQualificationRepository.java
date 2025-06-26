package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.InstructorClassQualification;
import com.example.studioapp_api.entity.InstructorClassQualificationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstructorClassQualificationRepository 
    extends JpaRepository<InstructorClassQualification, InstructorClassQualificationId> {

    List<InstructorClassQualification> findByIdInstructorId(Integer instructorId);
    List<InstructorClassQualification> findByIdClassDefinitionId(Integer classDefinitionId);
    // findByIdInstructorIdAndIdClassDefinitionId (already covered by findById(Id object))
}