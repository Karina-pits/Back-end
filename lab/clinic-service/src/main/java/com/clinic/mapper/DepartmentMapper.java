package com.clinic.mapper;

import com.clinic.dto.DepartmentDto;
import com.clinic.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DepartmentMapper {

    DepartmentDto toDto(Department entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "doctors", ignore = true)
    Department toEntity(DepartmentDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "doctors", ignore = true)
    void updateEntityFromDto(DepartmentDto dto, @MappingTarget Department entity);
}
