package com.gardening.timemanagement.mapper;

import com.gardening.timemanagement.dto.request.CreateEmployeeDto;
import com.gardening.timemanagement.dto.response.EmployeeResponseDto;
import com.gardening.timemanagement.entity.Employee;
import org.springframework.stereotype.Component;

/**
 * Mapper-klass för Employee-entiteten.
 * Konverterar mellan DTOs och Entities för clean separation of concerns.
 */
@Component
public class EmployeeMapper {

    /**
     * Konverterar från CreateEmployeeDto till Employee-entitet.
     */
    public Employee toEntity(CreateEmployeeDto dto) {
        if (dto == null) {
            return null;
        }

        Employee employee = new Employee();
        employee.setName(dto.getName());
        employee.setPhone(dto.getPhone());

        return employee;
    }

    /**
     * Konverterar från Employee-entitet till EmployeeResponseDto.
     */
    public EmployeeResponseDto toResponseDto(Employee employee) {
        if (employee == null) {
            return null;
        }

        EmployeeResponseDto dto = new EmployeeResponseDto();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        dto.setPhone(employee.getPhone());
        dto.setIsActive(employee.getIsActive());
        dto.setCanBeAssigned(employee.canBeAssignedToWork());
        dto.setCreatedAt(employee.getCreatedAt());
        dto.setUpdatedAt(employee.getUpdatedAt());

        return dto;
    }
}