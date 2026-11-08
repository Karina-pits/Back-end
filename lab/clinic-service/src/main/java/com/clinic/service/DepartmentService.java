package com.clinic.service;

import com.clinic.dto.DepartmentDto;

import java.util.List;

public interface DepartmentService {
    List<DepartmentDto> findAll();
    DepartmentDto findById(Long id);
    DepartmentDto create(DepartmentDto dto);
    DepartmentDto update(Long id, DepartmentDto dto);
    void delete(Long id);
}
