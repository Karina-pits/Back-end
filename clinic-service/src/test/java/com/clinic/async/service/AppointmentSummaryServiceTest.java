package com.clinic.async.service;

import com.clinic.dto.AppointmentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Чистий unit-тест без Spring: асинхронні сервіси замокані і повертають
 * готові (або навмисно проблемні) CompletableFuture.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentSummaryServiceTest {

    @Mock
    private PdfReportService pdfReportService;
    @Mock
    private InsuranceCheckService insuranceCheckService;

    private AppointmentSummaryService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentSummaryService(pdfReportService, insuranceCheckService, 200L);
    }

    private AppointmentDto appointment() {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(1L);
        dto.setPatientId(1L);
        dto.setDoctorId(1L);
        dto.setScheduledAt(LocalDateTime.of(2026, 11, 18, 10, 0));
        dto.setReason("Консультація");
        return dto;
    }

    @Test
    void buildSummary_bothCallsSucceed_combinesResultsIntoCompleteSummary() {
        // Arrange
        when(pdfReportService.generateAppointmentPdf(any()))
                .thenReturn(CompletableFuture.completedFuture("abc".getBytes()));
        when(insuranceCheckService.checkInsurance(any()))
                .thenReturn(CompletableFuture.completedFuture("ACTIVE"));

        // Act
        AppointmentSummary summary = service.buildSummary(appointment());

        // Assert
        assertThat(summary.pdfSizeBytes()).isEqualTo(3);
        assertThat(summary.insuranceStatus()).isEqualTo("ACTIVE");
        assertThat(summary.complete()).isTrue();
        assertThat(summary.note()).isNull();
    }

    @Test
    void buildSummary_insuranceCallFails_returnsPartialSummaryInsteadOfFailing() {
        // Arrange
        when(pdfReportService.generateAppointmentPdf(any()))
                .thenReturn(CompletableFuture.completedFuture("abc".getBytes()));
        when(insuranceCheckService.checkInsurance(any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("insurance api down")));

        // Act
        AppointmentSummary summary = service.buildSummary(appointment());

        // Assert: запит не впав, PDF є, страховка UNKNOWN
        assertThat(summary.pdfSizeBytes()).isEqualTo(3);
        assertThat(summary.insuranceStatus()).isEqualTo("UNKNOWN");
        assertThat(summary.complete()).isFalse();
        assertThat(summary.note()).isNotBlank();
    }

    @Test
    void buildSummary_pdfCallFails_returnsPartialSummaryInsteadOfFailing() {
        // Arrange
        when(pdfReportService.generateAppointmentPdf(any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("pdf engine crashed")));
        when(insuranceCheckService.checkInsurance(any()))
                .thenReturn(CompletableFuture.completedFuture("ACTIVE"));

        // Act
        AppointmentSummary summary = service.buildSummary(appointment());

        // Assert
        assertThat(summary.pdfSizeBytes()).isNull();
        assertThat(summary.insuranceStatus()).isEqualTo("ACTIVE");
        assertThat(summary.complete()).isFalse();
    }

    @Test
    void buildSummary_futureNeverCompletes_stopsWaitingAfterTimeout() {
        // Arrange: PDF "зависає" - Future ніколи не завершується
        when(pdfReportService.generateAppointmentPdf(any()))
                .thenReturn(new CompletableFuture<>());
        when(insuranceCheckService.checkInsurance(any()))
                .thenReturn(CompletableFuture.completedFuture("ACTIVE"));

        // Act
        long start = System.nanoTime();
        AppointmentSummary summary = service.buildSummary(appointment());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Assert: чекали приблизно timeout (200 мс), а не вічно
        assertThat(elapsedMs).isLessThan(2_000);
        assertThat(summary.complete()).isFalse();
        assertThat(summary.note()).contains("Час очікування");
    }
}
