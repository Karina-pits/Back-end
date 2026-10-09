package com.clinic.mapper;

import com.clinic.dto.PatientDto;
import com.clinic.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PatientMapper {

    PatientDto toDto(Patient entity);

    @Mapping(target = "id", ignore = true)
    Patient toEntity(PatientDto dto);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(PatientDto dto, @MappingTarget Patient entity);
}
