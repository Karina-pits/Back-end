package com.clinic.async.web;

import com.clinic.async.service.AppointmentSummary;
import com.clinic.async.service.AppointmentSummaryService;
import com.clinic.async.service.NotificationService;
import com.clinic.dto.AppointmentDto;
import com.clinic.dto.PatientDto;
import com.clinic.service.AppointmentService;
import com.clinic.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Доменні операції, що використовують повільні асинхронні побічні ефекти.
 * Доступ до запису перевіряє AppointmentService.findById (ролі та власник запису)
 * у потоці запиту - до запуску будь-яких асинхронних завдань.
 */
@Slf4j
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AsyncAppointmentController {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final NotificationService notificationService;
    private final AppointmentSummaryService appointmentSummaryService;

    /** Fire-and-forget: відповідь 202 приходить одразу, лист піде пізніше. */
    @PostMapping("/{id}/confirmation")
    public ResponseEntity<Map<String, String>> sendConfirmation(@PathVariable Long id) {
        AppointmentDto appointment = appointmentService.findById(id);
        PatientDto patient = patientService.findByIdForCurrentUser(appointment.getPatientId());
        if (patient.getEmail() == null || patient.getEmail().isBlank()) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("message", "У пацієнта не вказано email"));
        }
        notificationService.sendAppointmentConfirmation(appointment, patient.getEmail());
        return ResponseEntity.accepted()
                .body(Map.of("message", "Підтвердження поставлено в чергу на відправку"));
    }

    /** Результат потрібен одразу: два паралельні виклики об'єднуються в один підсумок. */
    @GetMapping("/{id}/summary")
    public AppointmentSummary summary(@PathVariable Long id) {
        AppointmentDto appointment = appointmentService.findById(id);
        return appointmentSummaryService.buildSummary(appointment);
    }

    /**
     * Явне рішення при переповненні виконавця: чітка відповідь "спробуйте пізніше"
     * (503 + Retry-After), а не випадкова помилка 500.
     */
    @ExceptionHandler(TaskRejectedException.class)
    public ResponseEntity<Map<String, String>> handleRejected(TaskRejectedException ex) {
        log.warn("[ASYNC] executor rejected task: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(Map.of(
                        "error", "Service Unavailable",
                        "message", "Сервер зараз перевантажений. Спробуйте, будь ласка, за кілька секунд"));
    }
}
