package com.clinic.service.impl;

import com.clinic.dto.PatientDto;
import com.clinic.entity.Patient;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.mapper.PatientMapper;
import com.clinic.repository.PatientRepository;
import com.clinic.security.SecurityUtils;
import com.clinic.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final SecurityUtils securityUtils;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @Transactional(readOnly = true)
    public List<PatientDto> findAll() {
        return patientRepository.findAll().stream()
                .map(patientMapper::toDto)
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @Transactional(readOnly = true)
    public PatientDto findById(Long id) {
        return patientMapper.toDto(getOrThrow(id));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PatientDto findByIdForCurrentUser(Long id) {
        Patient patient = getOrThrow(id);
        if (!securityUtils.isAdmin() && !securityUtils.isDoctor()
                && !patient.getKeycloakUserId().equals(securityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("Доступ заборонено: це не ваша картка пацієнта");
        }
        return patientMapper.toDto(patient);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PatientDto findCurrentUserProfile() {
        return patientRepository.findByKeycloakUserId(securityUtils.getCurrentUserId())
                .map(patientMapper::toDto)
                .orElse(null);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PatientDto findCurrentUserProfileByUserId(String userId) {
        return patientRepository.findByKeycloakUserId(userId)
                .map(patientMapper::toDto)
                .orElse(null);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public PatientDto create(PatientDto dto) {
        if (dto.getEmail() != null && patientRepository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new IllegalArgumentException("Пацієнт з таким email вже існує");
        }
        Patient entity = patientMapper.toEntity(dto);
        return patientMapper.toDto(patientRepository.save(entity));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public PatientDto update(Long id, PatientDto dto) {
        Patient entity = getOrThrow(id);
        patientMapper.updateEntityFromDto(dto, entity);
        return patientMapper.toDto(patientRepository.save(entity));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Patient entity = getOrThrow(id);
        patientRepository.delete(entity);
    }

    private Patient getOrThrow(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пацієнта не знайдено: id=" + id));
    }
}