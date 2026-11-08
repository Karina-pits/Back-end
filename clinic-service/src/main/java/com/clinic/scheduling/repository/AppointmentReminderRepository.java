package com.clinic.scheduling.repository;

import com.clinic.scheduling.entity.AppointmentReminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentReminderRepository extends JpaRepository<AppointmentReminder, Long> {

    boolean existsByAppointmentId(Long appointmentId);
}
