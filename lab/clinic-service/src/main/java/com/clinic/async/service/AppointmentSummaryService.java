package com.clinic.async.service;

import com.clinic.dto.AppointmentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Викликаючий код: запускає ДВА незалежні повільні виклики одночасно
 * (PDF і перевірка страховки), не чекаючи жодного з них, а потім
 * об'єднує результати через thenCombine в один підсумок.
 *
 * Стійкість до збоїв:
 *  - збій одного з викликів обробляється через handle/exceptionally,
 *    запит НЕ падає, а повертається неповний підсумок;
 *  - очікування завжди обмежене тайм-аутом (get з timeout), а не нескінченне.
 */
@Slf4j
@Service
public class AppointmentSummaryService {

    static final String UNKNOWN = "UNKNOWN";

    private final PdfReportService pdfReportService;
    private final InsuranceCheckService insuranceCheckService;
    private final long timeoutMs;

    public AppointmentSummaryService(PdfReportService pdfReportService,
                                     InsuranceCheckService insuranceCheckService,
                                     @Value("${clinic.async.summary-timeout-ms:3000}") long timeoutMs) {
        this.pdfReportService = pdfReportService;
        this.insuranceCheckService = insuranceCheckService;
        this.timeoutMs = timeoutMs;
    }

    public AppointmentSummary buildSummary(AppointmentDto appointment) {
        // Обидва виклики стартують одразу, ніхто нікого не чекає
        CompletableFuture<byte[]> pdfFuture = pdfReportService.generateAppointmentPdf(appointment);
        CompletableFuture<String> insuranceFuture =
                insuranceCheckService.checkInsurance(appointment.getPatientId());

        CompletableFuture<Integer> pdfSize = pdfFuture.handle((bytes, ex) -> {
            if (ex != null) {
                log.warn("[SUMMARY] PDF generation failed: {}", ex.toString());
                return null;
            }
            return bytes.length;
        });
        CompletableFuture<String> insuranceStatus = insuranceFuture.exceptionally(ex -> {
            log.warn("[SUMMARY] insurance check failed: {}", ex.toString());
            return UNKNOWN;
        });

        CompletableFuture<AppointmentSummary> combined = pdfSize.thenCombine(insuranceStatus,
                (size, status) -> {
                    boolean complete = size != null && !UNKNOWN.equals(status);
                    return new AppointmentSummary(appointment.getId(), size, status, complete,
                            complete ? null : "Частину даних отримати не вдалося");
                });

        try {
            return combined.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("[SUMMARY] timeout after {} ms for appointment {}", timeoutMs, appointment.getId());
            return degraded(appointment.getId(), "Час очікування вичерпано (" + timeoutMs + " мс)");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return degraded(appointment.getId(), "Очікування перервано");
        } catch (ExecutionException e) {
            log.error("[SUMMARY] unexpected failure", e);
            return degraded(appointment.getId(), "Не вдалося сформувати підсумок");
        }
    }

    private AppointmentSummary degraded(Long appointmentId, String note) {
        return new AppointmentSummary(appointmentId, null, UNKNOWN, false, note);
    }
}
