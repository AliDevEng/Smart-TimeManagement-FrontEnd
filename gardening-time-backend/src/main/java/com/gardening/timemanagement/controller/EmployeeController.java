package com.gardening.timemanagement.controller;

import com.gardening.timemanagement.dto.request.CreateEmployeeDto;
import com.gardening.timemanagement.dto.response.EmployeeResponseDto;
import com.gardening.timemanagement.entity.Employee;
import com.gardening.timemanagement.mapper.EmployeeMapper;
import com.gardening.timemanagement.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller för medarbetarhantering.
 *
 * Denna controller exponerar endpoints för:
 * - Skapa nya medarbetare
 * - Hämta medarbetarinformation
 * - Uppdatera medarbetare
 * - Hantera medarbetarstatus
 *
 * Följer REST-konventioner för HTTP-metoder och statuskoder.
 */

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeMapper employeeMapper;

    public EmployeeController(EmployeeService employeeService, EmployeeMapper employeeMapper) {
        this.employeeService = employeeService;
        this.employeeMapper = employeeMapper;
    }

    /**
     * Skapar en ny medarbetare.
     *
     * POST /api/employees
     *
     * @param createDto Data för den nya medarbetaren
     * @return Den skapade medarbetaren med HTTP 201 Created
     */
    @PostMapping
    public ResponseEntity<EmployeeResponseDto> createEmployee(@Valid @RequestBody CreateEmployeeDto createDto) {
        // 1. Konvertera DTO till Entity
        Employee employee = employeeMapper.toEntity(createDto);

        // 2. Anropa Service för affärslogik
        Employee savedEmployee = employeeService.createEmployee(employee);

        // 3. Konvertera Entity till Response DTO
        EmployeeResponseDto responseDto = employeeMapper.toResponseDto(savedEmployee);

        // 4. Returnera med korrekt HTTP-status
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Hämtar en specifik medarbetare baserat på ID.
     *
     * GET /api/employees/{id}
     *
     * @param id Medarbetarens ID
     * @return Medarbetarinformation eller HTTP 404 om inte hittad
     */

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponseDto> getEmployeeById(@PathVariable Long id) {
        // 1. Anropa Service för att hämta Employee
        Employee employee = employeeService.getEmployeeById(id);

        // 2. Konvertera Entity till Response DTO
        EmployeeResponseDto responseDto = employeeMapper.toResponseDto(employee);

        // 3. Returnera med HTTP 200 OK
        return ResponseEntity.ok(responseDto);
    }

}