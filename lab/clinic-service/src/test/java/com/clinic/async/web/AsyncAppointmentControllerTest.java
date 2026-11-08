package com.clinic.async.web;

import com.clinic.async.service.AppointmentSummary;
import com.clinic.async.service.AppointmentSummaryService;
import com.clinic.async.service.NotificationService;
import com.clinic.dto.AppointmentDto;
import com.clinic.dto.PatientDto;
import com.clinic.service.AppointmentService;
import com.clinic.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc: без Spring-контексту, сервіси замоканi.
 * Головне - переповнення виконавця дає чітку відповідь 503, а не випадкову 500.
 */
@ExtendWith(MockitoExtension.class)
class AsyncAppointmentControllerTest {

    @Mock
    private AppointmentService appointmentService;
    @Mock
    private PatientService patientService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AppointmentSummaryService appointmentSummaryService;

    @InjectMocks
    private AsyncAppointmentController controller;

    private MockMvc mockMvc;
    private AppointmentDto appointment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        appointment = new AppointmentDto();
        appointment.setId(1L);
        appointment.setPatientId(1L);
    }

    private PatientDto patientWithEmail(String email) {
        PatientDto patient = new PatientDto();
        patient.setEmail(email);
        return patient;
    }

    @Test
    void sendConfirmation_validAppointment_returns202AndStartsAsyncSending() throws Exception {
        when(appointmentService.findById(1L)).thenReturn(appointment);
        when(patientService.findByIdForCurrentUser(1L)).thenReturn(patientWithEmail("p@clinic.local"));

        mockMvc.perform(post("/api/appointments/1/confirmation"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());

        verify(notificationService).sendAppointmentConfirmation(appointment, "p@clinic.local");
    }

    @Test
    void sendConfirmation_patientWithoutEmail_returns422() throws Exception {
        when(appointmentService.findById(1L)).thenReturn(appointment);
        when(patientService.findByIdForCurrentUser(1L)).thenReturn(patientWithEmail(null));

        mockMvc.perform(post("/api/appointments/1/confirmation"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void sendConfirmation_executorRejectsTask_returns503WithRetryAfterInsteadOf500() throws Exception {
        when(appointmentService.findById(1L)).thenReturn(appointment);
        when(patientService.findByIdForCurrentUser(1L)).thenReturn(patientWithEmail("p@clinic.local"));
        doThrow(new TaskRejectedException("pool is full"))
                .when(notificationService).sendAppointmentConfirmation(any(), any());

        mockMvc.perform(post("/api/appointments/1/confirmation"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.message").value(containsString("Спробуйте")));
    }

    @Test
    void summary_validAppointment_returns200WithCombinedSummary() throws Exception {
        when(appointmentService.findById(1L)).thenReturn(appointment);
        when(appointmentSummaryService.buildSummary(appointment))
                .thenReturn(new AppointmentSummary(1L, 42, "ACTIVE", true, null));

        mockMvc.perform(get("/api/appointments/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insuranceStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.pdfSizeBytes").value(42))
                .andExpect(jsonPath("$.complete").value(true));
    }

    @Test
    void summary_executorRejectsTask_returns503InsteadOf500() throws Exception {
        when(appointmentService.findById(1L)).thenReturn(appointment);
        when(appointmentSummaryService.buildSummary(any()))
                .thenThrow(new TaskRejectedException("pool is full"));

        mockMvc.perform(get("/api/appointments/1/summary"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"));
    }
}
