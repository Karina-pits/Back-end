package com.clinic.mapper;

import com.clinic.dto.AppointmentDto;
import com.clinic.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientFullName", expression = "java(entity.getPatient().getFirstName() + \" \" + entity.getPatient().getLastName())")
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorFullName", expression = "java(entity.getDoctor().getFirstName() + \" \" + entity.getDoctor().getLastName())")
    AppointmentDto toDto(Appointment entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    Appointment toEntity(AppointmentDto dto);
}
