package com.clinic.service.impl;

import com.clinic.dto.DepartmentDto;
import com.clinic.entity.Department;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.mapper.DepartmentMapper;
import com.clinic.repository.DepartmentRepository;
import com.clinic.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDto> findAll() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto findById(Long id) {
        return departmentMapper.toDto(getOrThrow(id));
    }

    @Override
     @PreAuthorize("hasRole('ADMIN')")
    public DepartmentDto create(DepartmentDto dto) {
        if (departmentRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Відділення з такою назвою вже існує");
        }
        Department entity = departmentMapper.toEntity(dto);
        return departmentMapper.toDto(departmentRepository.save(entity));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentDto update(Long id, DepartmentDto dto) {
        Department entity = getOrThrow(id);
        departmentMapper.updateEntityFromDto(dto, entity);
        return departmentMapper.toDto(departmentRepository.save(entity));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Department entity = getOrThrow(id);
        departmentRepository.delete(entity);
    }

    private Department getOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Відділення не знайдено: id=" + id));
    }
}
