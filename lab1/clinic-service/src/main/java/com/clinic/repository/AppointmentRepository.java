package com.clinic.repository;

import com.clinic.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByDoctorId(Long doctorId);
    List<Appointment> findByPatientKeycloakUserId(String keycloakUserId);
    List<Appointment> findByDoctorKeycloakUserId(String keycloakUserId);
}
