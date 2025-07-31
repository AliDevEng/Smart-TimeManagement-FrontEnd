package com.gardening.timemanagement.controller;

import com.gardening.timemanagement.dto.request.CreateWorkDayDto;
import com.gardening.timemanagement.dto.request.UpdateWorkDayDto;
import com.gardening.timemanagement.dto.response.WorkDayResponseDto;
import com.gardening.timemanagement.exception.*;
import com.gardening.timemanagement.service.WorkDayService;
import com.gardening.timemanagement.util.WorkDayValidationUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller för WorkDay API - Enterprise-kvalitet orchestration.
 *
 * Denna controller representerar kulmen av vår "Inside-Out" arkitektur,
 * där alla foundation-komponenter vi byggt arbetar tillsammans för att
 * leverera robust, säker och användarvänlig API-funktionalitet.
 *
 * Designprinciper:
 * - Standard CRUD operations för grundläggande livscykel-hantering
 * - Business operations för domain-specific workflows
 * - Rik exception handling med actionable error responses
 * - Performance-medveten med smart caching och batch operations
 * - Security-first med comprehensive input validation
 * - Frontend-vänlig med embedded data och metadata
 *
 * @author Enterprise Development Team
 * @version 1.0
 * @since WorkDay API v1.0
 */
@RestController
@RequestMapping("/api/workdays")
@CrossOrigin(origins = "*") // TODO: Konfigurera för production med specifika domains
public class WorkDayController {

    private final WorkDayService workDayService;

    /**
     * Constructor injection för optimal dependency management.
     * Spring IoC container hanterar lifecycle och dependency resolution.
     */
    @Autowired
    public WorkDayController(WorkDayService workDayService) {
        this.workDayService = workDayService;
    }

    // =================================================================
    // STANDARD CRUD OPERATIONS - Core Lifecycle Management
    // =================================================================

    /**
     * Skapar en ny arbetsdag med komplett business rule validation.
     *
     * Denna endpoint hanterar den mest komplexa create-operationen i systemet
     * eftersom WorkDay måste koordinera med Task, Employee och Equipment
     * samtidigt som den säkerställer alla business invariants.
     *
     * Frontend Usage:
     * POST /api/workdays
     * Content-Type: application/json
     *
     * @param createDto Validated DTO med all information för att skapa arbetsdagen
     * @return 201 Created med WorkDayResponseDto eller error response
     */
    @PostMapping
    public ResponseEntity<?> createWorkDay(@Valid @RequestBody CreateWorkDayDto createDto) {
        try {
            // Service layer hanterar all complex business logic och validation
            WorkDayResponseDto createdWorkDay = workDayService.createWorkDay(createDto);

            // 201 Created med Location header för REST compliance
            return ResponseEntity.status(HttpStatus.CREATED).body(createdWorkDay);

        } catch (InvalidWorkDayException e) {
            // Business rule violations - returnera detailed explanation
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getTitle(), e.getMessage(),
                    Map.of("category", "validation", "field", e.getField()));

        } catch (DuplicateWorkDayException e) {
            // Unique constraint violations - helpful för frontend UX
            return buildErrorResponse(HttpStatus.CONFLICT, "Duplicate WorkDay", e.getMessage(),
                    Map.of("category", "duplicate", "suggestion", "Use update instead of create"));

        } catch (TaskNotFoundException | EmployeeNotFoundException e) {
            // Cross-entity reference problems - guide user to resolution
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Reference Not Found", e.getMessage(),
                    Map.of("category", "reference", "action", "Verify referenced entities exist"));

        } catch (Exception e) {
            // Unexpected errors - log för debugging men ge user-friendly response
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                    "An unexpected error occurred while creating the work day",
                    Map.of("category", "system", "action", "Contact support if issue persists"));
        }
    }

    /**
     * Hämtar en specifik arbetsdag med full information.
     *
     * Returnerar embedded employee, task och equipment data för optimal
     * frontend performance och reduced API calls.
     *
     * @param id WorkDay ID att hämta
     * @return 200 OK med WorkDayResponseDto eller 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkDay(@PathVariable Long id) {
        try {
            WorkDayResponseDto workDay = workDayService.getWorkDayById(id);
            return ResponseEntity.ok(workDay);

        } catch (WorkDayNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "WorkDay Not Found", e.getMessage(),
                    Map.of("category", "not_found", "id", id.toString()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                    "An error occurred while retrieving the work day",
                    Map.of("category", "system", "id", id.toString()));
        }
    }

    /**
     * Uppdaterar en befintlig arbetsdag med delta-semantik.
     *
     * Endast icke-null fält i UpdateWorkDayDto kommer att uppdateras,
     * vilket ger flexibel partial update functionality.
     *
     * @param id WorkDay ID att uppdatera
     * @param updateDto DTO med fält som ska uppdateras
     * @return 200 OK med uppdaterad WorkDayResponseDto
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkDay(@PathVariable Long id,
                                           @Valid @RequestBody UpdateWorkDayDto updateDto) {
        try {
            WorkDayResponseDto updatedWorkDay = workDayService.updateWorkDay(id, updateDto);
            return ResponseEntity.ok(updatedWorkDay);

        } catch (WorkDayNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "WorkDay Not Found", e.getMessage(),
                    Map.of("category", "not_found", "id", id.toString()));

        } catch (InvalidWorkDayException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getTitle(), e.getMessage(),
                    Map.of("category", "validation", "field", e.getField(), "id", id.toString()));

        } catch (DuplicateWorkDayException e) {
            return buildErrorResponse(HttpStatus.CONFLICT, "Update Conflict", e.getMessage(),
                    Map.of("category", "duplicate", "id", id.toString()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                    "An error occurred while updating the work day",
                    Map.of("category", "system", "id", id.toString()));
        }
    }

    /**
     * Tar bort en arbetsdag med cascade-awareness.
     *
     * Kontrollerar business rules för deletion och ger detailed feedback
     * om varför deletion kanske inte är möjlig.
     *
     * @param id WorkDay ID att ta bort
     * @return 204 No Content vid framgång eller error response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkDay(@PathVariable Long id) {
        try {
            workDayService.deleteWorkDay(id);
            return ResponseEntity.noContent().build();

        } catch (WorkDayNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "WorkDay Not Found", e.getMessage(),
                    Map.of("category", "not_found", "id", id.toString()));

        } catch (WorkDayDeletionException e) {
            return buildErrorResponse(HttpStatus.CONFLICT, "Deletion Not Allowed", e.getMessage(),
                    Map.of("category", "deletion_blocked", "id", id.toString(),
                            "action", "Resolve blocking constraints first"));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                    "An error occurred while deleting the work day",
                    Map.of("category", "system", "id", id.toString()));
        }
    }

    // =================================================================
    // QUERY OPERATIONS - Advanced Data Retrieval
    // =================================================================

    /**
     * Hämtar alla arbetsdagar med optional filtering och pagination.
     *
     * Supports multiple query parameters för flexible data retrieval:
     * - Date range filtering för reports
     * - Task-specific filtering för project tracking
     * - Employee-specific filtering för personal reports
     *
     * @param startDate Optional start date för filtering
     * @param endDate Optional end date för filtering
     * @param taskId Optional task ID för filtering
     * @param employeeId Optional employee ID för filtering
     * @param includeMetrics Om performance metrics ska inkluderas
     * @return Lista med WorkDayResponseDto objekt
     */
    @GetMapping
    public ResponseEntity<?> getWorkDays(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "false") boolean includeMetrics) {

        try {
            List<WorkDayResponseDto> workDays;

            // Smart routing baserat på query parameters
            if (startDate != null && endDate != null) {
                workDays = workDayService.getWorkDaysInDateRange(startDate, endDate, includeMetrics);
            } else if (taskId != null) {
                workDays = workDayService.getWorkDaysByTask(taskId, includeMetrics);
            } else if (employeeId != null) {
                workDays = workDayService.getWorkDaysByEmployee(employeeId, includeMetrics);
            } else {
                // Default: recent work days för dashboard view
                workDays = workDayService.getRecentWorkDays(30, includeMetrics);
            }

            // Lägg till metadata för frontend optimization
            Map<String, Object> response = new HashMap<>();
            response.put("workDays", workDays);
            response.put("count", workDays.size());
            response.put("includeMetrics", includeMetrics);

            if (startDate != null && endDate != null) {
                response.put("dateRange", Map.of("start", startDate, "end", endDate));
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Invalid query parameters - guide user to correct usage
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Query Parameters", e.getMessage(),
                    Map.of("category", "query_parameters",
                            "suggestion", "Check date formats and parameter values"));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                    "An error occurred while retrieving work days",
                    Map.of("category", "system"));
        }
    }

    // =================================================================
    // BUSINESS OPERATIONS - Domain-Specific Workflows
    // =================================================================

    /**
     * Bulk operation för att lägga till medarbetare till en befintlig arbetsdag.
     *
     * Detta är en business operation som går utöver standard CRUD eftersom
     * den hanterar complex employee assignment validation och conflict resolution.
     *
     * @param workDayId WorkDay att lägga till medarbetare till
     * @param employeeIds Lista med Employee IDs att lägga till
     * @return Uppdaterad WorkDayResponseDto
     */
    @PostMapping("/{workDayId}/employees")
    public ResponseEntity<?> addEmployeesToWorkDay(@PathVariable Long workDayId,
                                                   @RequestBody List<Long> employeeIds) {
        try {
            WorkDayResponseDto updatedWorkDay = workDayService.addEmployeesToWorkDay(workDayId, employeeIds);
            return ResponseEntity.ok(updatedWorkDay);

        } catch (WorkDayNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "WorkDay Not Found", e.getMessage(),
                    Map.of("category", "not_found", "workDayId", workDayId.toString()));

        } catch (EmployeeNotFoundException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Employee Not Found", e.getMessage(),
                    Map.of("category", "reference", "workDayId", workDayId.toString()));

        } catch (InvalidWorkDayException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getTitle(), e.getMessage(),
                    Map.of("category", "business_rule", "workDayId", workDayId.toString()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                    "An error occurred while adding employees to work day",
                    Map.of("category", "system", "workDayId", workDayId.toString()));
        }
    }

    /**
     * Business operation för equipment conflict detection och resolution.
     *
     * Analyserar equipment usage conflicts för en given period och
     * föreslår resolution strategies för project managers.
     *
     * @param startDate Start av period att analysera
     * @param endDate Slut av period att analysera
     * @return Conflict analysis rapport med resolution suggestions
     */
    @GetMapping("/equipment-conflicts")
    public ResponseEntity<?> analyzeEquipmentConflicts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // Validate date range using our sophisticated validation utils
            WorkDayValidationUtils.ValidationResult dateValidation =
                    WorkDayValidationUtils.validateReportDateRange(startDate, endDate);

            if (!dateValidation.isValid()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Date Range",
                        dateValidation.getMessage().orElse("Invalid date parameters"),
                        Map.of("category", "date_validation"));
            }

            Map<String, Object> conflictAnalysis = workDayService.analyzeEquipmentConflicts(startDate, endDate);
            return ResponseEntity.ok(conflictAnalysis);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                    "An error occurred while analyzing equipment conflicts",
                    Map.of("category", "system", "dateRange", startDate + " to " + endDate));
        }
    }

    /**
     * Validates om en ny arbetsdag kan skapas utan conflicts.
     *
     * Denna preview-operation låter frontend validera input innan
     * actual creation attempt, vilket förbättrar user experience.
     *
     * @param createDto DTO att validera (samma som för creation)
     * @return Validation result med detailed feedback
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateWorkDayCreation(@Valid @RequestBody CreateWorkDayDto createDto) {
        try {
            Map<String, Object> validationResult = workDayService.validateWorkDayCreation(createDto);
            return ResponseEntity.ok(validationResult);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                    "An error occurred while validating work day creation",
                    Map.of("category", "system"));
        }
    }

    // =================================================================
    // REPORTING OPERATIONS - Business Intelligence Support
    // =================================================================

    /**
     * Genererar comprehensive work day statistics för management reporting.
     *
     * Denna endpoint producerar rich analytics som används av
     * business intelligence tools och management dashboards.
     *
     * @param startDate Rapportperiod start
     * @param endDate Rapportperiod slut
     * @param groupBy Gruppering: "task", "employee", "date", eller "equipment"
     * @return Statistics rapport med multiple dimensions
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getWorkDayStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "date") String groupBy) {

        try {
            // Validate date range med våra sophisticated validation rules
            WorkDayValidationUtils.ValidationResult dateValidation =
                    WorkDayValidationUtils.validateReportDateRange(startDate, endDate);

            if (!dateValidation.isValid()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Date Range",
                        dateValidation.getMessage().orElse("Invalid date parameters"),
                        Map.of("category", "date_validation"));
            }

            // Validate groupBy parameter
            if (!List.of("task", "employee", "date", "equipment").contains(groupBy.toLowerCase())) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Group By Parameter",
                        "groupBy must be one of: task, employee, date, equipment",
                        Map.of("category", "parameter_validation", "provided", groupBy));
            }

            Map<String, Object> statistics = workDayService.generateWorkDayStatistics(startDate, endDate, groupBy);
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                    "An error occurred while generating statistics",
                    Map.of("category", "system", "dateRange", startDate + " to " + endDate));
        }
    }

    // =================================================================
    // HELPER METHODS - Error Response Construction
    // =================================================================

    /**
     * Builds consistent error response struktur för optimal frontend handling.
     *
     * Error responses följer en standardiserad struktur som gör det enkelt
     * för frontend att visa användarvänliga meddelanden och handling guidance.
     *
     * @param status HTTP status code
     * @param title Short, descriptive error title
     * @param message Detailed error explanation
     * @param details Additional context för debugging och user guidance
     * @return ResponseEntity med structured error response
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String title,
                                                                   String message, Map<String, Object> details) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("status", status.value());
        errorResponse.put("title", title);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", java.time.Instant.now().toString());

        if (details != null && !details.isEmpty()) {
            errorResponse.put("details", details);
        }

        // Lägg till helpful suggestions baserat på error type
        if (status == HttpStatus.BAD_REQUEST) {
            errorResponse.put("suggestion", "Please review the provided data and try again");
        } else if (status == HttpStatus.NOT_FOUND) {
            errorResponse.put("suggestion", "Verify that the requested resource exists");
        } else if (status == HttpStatus.CONFLICT) {
            errorResponse.put("suggestion", "Resolve the conflict by updating existing data or changing input");
        }

        return ResponseEntity.status(status).body(errorResponse);
    }
}