package com.clinic.service;

import com.clinic.dto.DoctorDto;

import java.util.List;

public interface DoctorService {
    List<DoctorDto> findAll();
    DoctorDto findById(Long id);
    DoctorDto create(DoctorDto dto);
    DoctorDto update(Long id, DoctorDto dto);
    void delete(Long id);
}
