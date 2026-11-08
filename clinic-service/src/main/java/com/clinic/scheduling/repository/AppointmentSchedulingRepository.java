package com.clinic.scheduling.repository;

import com.clinic.entity.Appointment;
import com.clinic.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Запити, потрібні лише планувальнику. Винесені в окремий репозиторій,
 * щоб не змінювати наявний AppointmentRepository.
 */
public interface AppointmentSchedulingRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByStatusAndScheduledAtBetween(AppointmentStatus status,
                                                        LocalDateTime from,
                                                        LocalDateTime to);

    List<Appointment> findByStatusAndScheduledAtBefore(AppointmentStatus status,
                                                       LocalDateTime before);
}
