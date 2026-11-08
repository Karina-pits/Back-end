package com.clinic.service;

import com.clinic.dto.AppointmentDto;
import com.clinic.entity.AppointmentStatus;

import java.util.List;

public interface AppointmentService {
    List<AppointmentDto> findAllVisibleToCurrentUser();
    AppointmentDto findById(Long id);
    AppointmentDto create(AppointmentDto dto);
    AppointmentDto update(Long id, AppointmentDto dto);
    AppointmentDto updateStatus(Long id, AppointmentStatus status);
    void cancel(Long id);
    void delete(Long id);
}


//список методів для управління прийомами до лікаря