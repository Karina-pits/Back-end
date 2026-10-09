package com.clinic.mapper;

import com.clinic.dto.DoctorDto;
import com.clinic.entity.Doctor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DoctorMapper {

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    DoctorDto toDto(Doctor entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    Doctor toEntity(DoctorDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    void updateEntityFromDto(DoctorDto dto, @MappingTarget Doctor entity);
}
