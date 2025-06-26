package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.InstructorPriorityRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstructorPriorityRequestRepository 
    extends JpaRepository<InstructorPriorityRequest, Integer> {

    List<InstructorPriorityRequest> findByInstructorId(Integer instructorId);
    List<InstructorPriorityRequest> findByTermId(Integer termId);
    List<InstructorPriorityRequest> findByInstructorIdAndTermId(Integer instructorId, Integer termId);
    // Add more specific finders as needed, e.g., by isActive status, by organization via instructor/term
}