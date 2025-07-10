package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.SolverJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolverJobRepository extends JpaRepository<SolverJob, String> {
}
