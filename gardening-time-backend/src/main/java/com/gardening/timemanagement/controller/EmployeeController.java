package com.gardening.timemanagement.controller;

import com.gardening.timemanagement.dto.request.CreateEmployeeDto;
import com.gardening.timemanagement.dto.request.EmployeeStatusChangeDto;
import com.gardening.timemanagement.dto.request.UpdateEmployeeDto;
import com.gardening.timemanagement.dto.response.EmployeeResponseDto;
import com.gardening.timemanagement.entity.Employee;
import com.gardening.timemanagement.mapper.EmployeeMapper;
import com.gardening.timemanagement.service.EmployeeService;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * Uppdaterar en befintlig medarbetare.
     *
     * PUT /api/employees/{id}
     *
     * @param id Medarbetarens ID
     * @param updateDto Uppdaterad medarbetarinformation
     * @return Den uppdaterade medarbetaren
     */
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponseDto> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeDto updateDto) {

        // 1. Service hanterar all affärslogik och mappning
        Employee updatedEmployee = employeeService.updateEmployee(id, updateDto);

        // 2. Konvertera till Response DTO
        EmployeeResponseDto responseDto = employeeMapper.toResponseDto(updatedEmployee);

        // 3. Returnera med HTTP 200 OK
        return ResponseEntity.ok(responseDto);
    }

    // Hämta alla medarbetare
    // GET /api/employees
    @GetMapping
    public ResponseEntity<List<EmployeeResponseDto>> getAllEmployees() {
        // 1. Hämta all medarbetare från Service
        List<Employee> employees = employeeService.getAllEmployees();

        // 2. Konvertera alla till Response DTOs
        List<EmployeeResponseDto> responseDtos = employees.stream()
                .map(employeeMapper::toResponseDto)
                .toList();

        // 3. Returnera med HTTP 200 OK
        return ResponseEntity.ok(responseDtos);
    }


    // Inaktivera en medarbetare
    // PATCH /api/employees/{id}/deactivate
    // PATCH = partiell uppdatering
    // @RequestBody(required = false) = orsaken är valfri

    @PatchMapping ("/{id}/deactivate")
    public ResponseEntity<EmployeeResponseDto> deactivateEmployee (
            @PathVariable Long id,
            @RequestBody(required = false)EmployeeStatusChangeDto statusChangeDto) {

        // 1. Hämta orsak från DTO (kan vara null)
        String reason = statusChangeDto != null ? statusChangeDto.getReason() : null;

        // 2 Anropa Service för inaktivering
        Employee deactivatedEmployee = employeeService.deactivateEmployee(id, reason);

        // 3. Konvertera till Response DTO
        EmployeeResponseDto responseDto = employeeMapper.toResponseDto(deactivatedEmployee);

        // 4. Returnera med HTTP 200 OK
        return ResponseEntity.ok(responseDto);
    }


    // Återaktivera en medarbetare
    // PATCH /api/employees/{id}/reactivate

    @PatchMapping ("/{id}/reactivate")
    public ResponseEntity<EmployeeResponseDto> reactivateEmployee (
            @PathVariable Long id,
            @RequestBody(required = false)EmployeeStatusChangeDto statusChangeDto) {

        // 1. Hämta orsak från DTO (kan vara null)
        String reason = statusChangeDto != null ? statusChangeDto.getReason() : null;

        // 2 Anropa Service för inaktivering
        Employee reactivatedEmployee = employeeService.reactivateEmployee(id, reason);

        // 3. Konvertera till Response DTO
        EmployeeResponseDto responseDto = employeeMapper.toResponseDto(reactivatedEmployee);

        // 4. Returnera med HTTP 200 OK
        return ResponseEntity.ok(responseDto);
    }

}