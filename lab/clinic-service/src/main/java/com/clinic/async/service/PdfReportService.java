package com.clinic.async.service;

import com.clinic.async.config.AsyncConfig;
import com.clinic.async.gateway.SimulatedWork;
import com.clinic.dto.AppointmentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Повільний побічний ефект №2: генерування PDF-звіту про запис на прийом.
 * Зараз це імітація (повертає байти тексту); у реальному проєкті тут була б
 * бібліотека на кшталт OpenPDF.
 *
 * Результат потрібен викликаючому коду, тому метод повертає CompletableFuture.
 */
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final SimulatedWork work;

    @Async(AsyncConfig.CLINIC_EXECUTOR)
    public CompletableFuture<byte[]> generateAppointmentPdf(AppointmentDto appointment) {
        work.pause();
        String content = "PDF-SIMULATED appointment id=" + appointment.getId()
                + " scheduledAt=" + appointment.getScheduledAt()
                + " reason=" + appointment.getReason();
        return CompletableFuture.completedFuture(content.getBytes(StandardCharsets.UTF_8));
    }
}
