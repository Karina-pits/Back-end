package com.clinic.controller;

import com.clinic.dto.PatientDto;
import com.clinic.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;


    @GetMapping
    public List<PatientDto> findAll() {
        return patientService.findAll();
    }

     @GetMapping("/{id}")
    public PatientDto findById(@PathVariable Long id) {
        return patientService.findByIdForCurrentUser(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientDto create(@Valid @RequestBody PatientDto dto) {
        return patientService.create(dto);
    }

    @PutMapping("/{id}")
    public PatientDto update(@PathVariable Long id, @Valid @RequestBody PatientDto dto) {
        return patientService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
