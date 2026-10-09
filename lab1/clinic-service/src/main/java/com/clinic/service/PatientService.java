package com.clinic.service;

import com.clinic.dto.PatientDto;

import java.util.List;

public interface PatientService {
    List<PatientDto> findAll();
    PatientDto findById(Long id);
    PatientDto findByIdForCurrentUser(Long id);
    PatientDto findCurrentUserProfile();
    PatientDto findCurrentUserProfileByUserId(String userId);   // ← додай цей рядок
    PatientDto create(PatientDto dto);
    PatientDto update(Long id, PatientDto dto);
    void delete(Long id);
}