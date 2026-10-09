package com.clinic.controller;

import com.clinic.dto.AppointmentDto;
import com.clinic.entity.AppointmentStatus;
import com.clinic.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public List<AppointmentDto> findAll() {
        return appointmentService.findAllVisibleToCurrentUser();
    }

    @GetMapping("/{id}")
    public AppointmentDto findById(@PathVariable Long id) {
        return appointmentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentDto create(@Valid @RequestBody AppointmentDto dto) {
        return appointmentService.create(dto);
    }

    @PutMapping("/{id}")
    public AppointmentDto update(@PathVariable Long id, @Valid @RequestBody AppointmentDto dto) {
        return appointmentService.update(id, dto);
    }

    @PatchMapping("/{id}/status")
    public AppointmentDto updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AppointmentStatus status = AppointmentStatus.valueOf(body.get("status").toUpperCase());
        return appointmentService.updateStatus(id, status);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentDto cancel(@PathVariable Long id) {
        appointmentService.cancel(id);
        return appointmentService.findById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        appointmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
