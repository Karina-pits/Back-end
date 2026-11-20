package com.clinic.notification.repository;

import com.clinic.notification.entity.AppointmentNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentNotificationRepository extends JpaRepository<AppointmentNotification, Long> {

    boolean existsByEventId(String eventId);

    List<AppointmentNotification> findAllByOrderByIdAsc();
}
