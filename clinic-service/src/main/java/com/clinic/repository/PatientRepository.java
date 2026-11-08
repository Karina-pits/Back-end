package com.clinic.repository;

import com.clinic.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByKeycloakUserId(String keycloakUserId);
    boolean existsByEmailIgnoreCase(String email);
}
