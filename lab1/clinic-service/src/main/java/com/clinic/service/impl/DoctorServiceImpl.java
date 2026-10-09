package com.clinic.service.impl;

import com.clinic.dto.DoctorDto;
import com.clinic.entity.Department;
import com.clinic.entity.Doctor;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.mapper.DoctorMapper;
import com.clinic.repository.DepartmentRepository;
import com.clinic.repository.DoctorRepository;
import com.clinic.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorMapper doctorMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DoctorDto> findAll() {
        return doctorRepository.findAll().stream()
                .map(doctorMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorDto findById(Long id) {
        return doctorMapper.toDto(getOrThrow(id));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public DoctorDto create(DoctorDto dto) {
        if (doctorRepository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new IllegalArgumentException("Лікар з таким email вже існує");
        }
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Відділення не знайдено: id=" + dto.getDepartmentId()));
        Doctor entity = doctorMapper.toEntity(dto);
        entity.setDepartment(department);
        return doctorMapper.toDto(doctorRepository.save(entity));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public DoctorDto update(Long id, DoctorDto dto) {
        Doctor entity = getOrThrow(id);
        doctorMapper.updateEntityFromDto(dto, entity);
        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Відділення не знайдено: id=" + dto.getDepartmentId()));
            entity.setDepartment(department);
        }
        return doctorMapper.toDto(doctorRepository.save(entity));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Doctor entity = getOrThrow(id);
        doctorRepository.delete(entity);
    }

    private Doctor getOrThrow(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Лікаря не знайдено: id=" + id));
    }
}
