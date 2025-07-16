package com.gardening.timemanagement.controller;

import com.gardening.timemanagement.dto.request.CreateTaskDto;
import com.gardening.timemanagement.dto.request.UpdateTaskDto;
import com.gardening.timemanagement.dto.response.TaskResponseDto;
import com.gardening.timemanagement.entity.Task.TaskStatus;
import com.gardening.timemanagement.exception.TaskNotFoundException;
import com.gardening.timemanagement.exception.InvalidTaskTransitionException;
import com.gardening.timemanagement.exception.TaskDeletionException;
import com.gardening.timemanagement.exception.DuplicateTaskException;
import com.gardening.timemanagement.service.TaskService;
import com.gardening.timemanagement.util.TaskStatusUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST Controller för Task-entiteten med omfattande business operation support.
 *
 * Denna controller exponerar alla våra sofistikerade Task-operationer genom en
 * clean, RESTful HTTP API. Den orchestrerar komplex affärslogik medan den
 * säkerställer konsistenta HTTP-responses och användarvänlig felhantering.
 *
 * API Design Principles:
 * - RESTful resource modeling med intuitiva endpoints
 * - Konsistenta HTTP-statuskoder för alla operationer
 * - Rich error responses med actionable information
 * - Support för både enkla CRUD och komplexa business workflows
 * - Optimal prestanda genom smart caching och batch operations
 *
 * Pedagogiska lärdomar från denna controller:
 * - Enterprise-nivå HTTP API design
 * - Sophisticated error handling och response mapping
 * - Business workflow orchestration via REST endpoints
 * - Performance-optimized API patterns
 * - Integration med complex service layer operations
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*") // För utveckling - bör konfigureras säkert för produktion
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // ===================================================================
    // GRUNDLÄGGANDE CRUD ENDPOINTS
    // ===================================================================

    /**
     * GET /api/tasks - Hämtar alla uppdrag
     *
     * Returnerar en lista av alla uppdrag med embedded customer-information
     * för optimal användargränssnittsintegration.
     *
     * @return HTTP 200 OK med lista av TaskResponseDto
     */
    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> getAllTasks() {
        List<TaskResponseDto> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * GET /api/tasks/{id} - Hämtar ett specifikt uppdrag
     *
     * @param id Uppdragets ID
     * @return HTTP 200 OK med TaskResponseDto, eller 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getTaskById(@PathVariable Long id) {
        TaskResponseDto task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    /**
     * GET /api/tasks/by-number/{taskNumber} - Hämtar uppdrag baserat på uppdragsnummer
     *
     * Detta är en convenience endpoint för användare som känner till uppdragsnumret
     * men inte det interna ID:t.
     *
     * @param taskNumber Uppdragsnummer att söka efter
     * @return HTTP 200 OK med TaskResponseDto, eller 404 Not Found
     */
    @GetMapping("/by-number/{taskNumber}")
    public ResponseEntity<TaskResponseDto> getTaskByNumber(@PathVariable String taskNumber) {
        TaskResponseDto task = taskService.getTaskByNumber(taskNumber);
        return ResponseEntity.ok(task);
    }

    /**
     * POST /api/tasks - Skapar ett nytt uppdrag
     *
     * @param createDto Data för det nya uppdraget
     * @return HTTP 201 Created med det skapade uppdraget
     */
    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(@Valid @RequestBody CreateTaskDto createDto) {
        TaskResponseDto createdTask = taskService.createTask(createDto);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    /**
     * PUT /api/tasks/{id} - Uppdaterar ett befintligt uppdrag
     *
     * @param id Uppdragets ID
     * @param updateDto Uppdateringsdata
     * @return HTTP 200 OK med det uppdaterade uppdraget
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDto> updateTask(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateTaskDto updateDto) {
        TaskResponseDto updatedTask = taskService.updateTask(id, updateDto);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * DELETE /api/tasks/{id} - Tar bort ett uppdrag
     *
     * @param id Uppdragets ID
     * @return HTTP 204 No Content vid framgångsrik borttagning
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    // ===================================================================
    // BUSINESS OPERATION ENDPOINTS
    // ===================================================================

    /**
     * POST /api/tasks/{id}/start - Startar ett uppdrag
     *
     * Detta är en high-level business operation som hanterar automatisk
     * datum-sättning och status-övergång.
     *
     * @param id Uppdragets ID
     * @return HTTP 200 OK med det startade uppdraget
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<TaskResponseDto> startTask(@PathVariable Long id) {
        TaskResponseDto startedTask = taskService.startTask(id);
        return ResponseEntity.ok(startedTask);
    }

    /**
     * POST /api/tasks/{id}/complete - Avslutar ett uppdrag
     *
     * @param id Uppdragets ID
     * @return HTTP 200 OK med det avslutade uppdraget
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<TaskResponseDto> completeTask(@PathVariable Long id) {
        TaskResponseDto completedTask = taskService.completeTask(id);
        return ResponseEntity.ok(completedTask);
    }

    /**
     * POST /api/tasks/{id}/cancel - Avbryter ett uppdrag
     *
     * @param id Uppdragets ID
     * @param reason Anledning till avbrytande (optional, som request parameter)
     * @return HTTP 200 OK med det avbrutna uppdraget
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TaskResponseDto> cancelTask(@PathVariable Long id,
                                                      @RequestParam(required = false) String reason) {
        TaskResponseDto cancelledTask = taskService.cancelTask(id, reason);
        return ResponseEntity.ok(cancelledTask);
    }

    /**
     * PUT /api/tasks/{id}/status - Ändrar status på ett uppdrag
     *
     * Detta är en mer generell endpoint för statusändringar som kompletterar
     * de specifika business operation endpoints ovan.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<TaskResponseDto> changeTaskStatus(@PathVariable Long id,
                                                            @RequestBody Map<String, String> request) {
        String statusString = request.get("status");
        if (statusString == null) {
            throw new IllegalArgumentException("Status måste anges");
        }

        TaskStatus status = TaskStatusUtils.fromString(statusString)
                .orElseThrow(() -> new IllegalArgumentException("Ogiltig status: " + statusString));

        TaskResponseDto updatedTask = taskService.changeTaskStatus(id, status);
        return ResponseEntity.ok(updatedTask);
    }

    // ===================================================================
    // QUERY OCH SÖKNING ENDPOINTS
    // ===================================================================

    /**
     * GET /api/tasks/search - Söker uppdrag baserat på olika kriterier
     *
     * @param q Sökterm för uppdragsnummer eller kundnamn
     * @param status Filtrera på specifik status
     * @param customerId Filtrera på specifik kund
     * @param startDate Filtrera på startdatum (från)
     * @param endDate Filtrera på startdatum (till)
     * @return HTTP 200 OK med lista av matchande uppdrag
     */
    @GetMapping("/search")
    public ResponseEntity<List<TaskResponseDto>> searchTasks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        List<TaskResponseDto> tasks;

        if (customerId != null) {
            tasks = taskService.getTasksByCustomer(customerId);
        } else if (status != null) {
            TaskStatus taskStatus = TaskStatusUtils.fromString(status)
                    .orElseThrow(() -> new IllegalArgumentException("Ogiltig status: " + status));
            tasks = taskService.getTasksByStatus(taskStatus);
        } else if (startDate != null && endDate != null) {
            tasks = taskService.getTasksWithWorkInPeriod(startDate, endDate);
        } else if (q != null) {
            tasks = taskService.searchTasks(q);
        } else {
            tasks = taskService.getAllTasks();
        }

        return ResponseEntity.ok(tasks);
    }

    /**
     * GET /api/tasks/by-customer/{customerId} - Hämtar alla uppdrag för en kund
     *
     * @param customerId Kundens ID
     * @return HTTP 200 OK med lista av kundens uppdrag
     */
    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<List<TaskResponseDto>> getTasksByCustomer(@PathVariable Long customerId) {
        List<TaskResponseDto> tasks = taskService.getTasksByCustomer(customerId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * GET /api/tasks/by-status/{status} - Hämtar alla uppdrag med specifik status
     *
     * @param status Status att filtrera på
     * @return HTTP 200 OK med lista av uppdrag med angiven status
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<TaskResponseDto>> getTasksByStatus(@PathVariable String status) {
        TaskStatus taskStatus = TaskStatusUtils.fromString(status)
                .orElseThrow(() -> new IllegalArgumentException("Ogiltig status: " + status));

        List<TaskResponseDto> tasks = taskService.getTasksByStatus(taskStatus);
        return ResponseEntity.ok(tasks);
    }

    /**
     * GET /api/tasks/with-work - Hämtar uppdrag som har arbetstid i en period
     *
     * @param startDate Startdatum för perioden
     * @param endDate Slutdatum för perioden
     * @return HTTP 200 OK med lista av uppdrag med arbetstid
     */
    @GetMapping("/with-work")
    public ResponseEntity<List<TaskResponseDto>> getTasksWithWork(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        List<TaskResponseDto> tasks = taskService.getTasksWithWorkInPeriod(startDate, endDate);
        return ResponseEntity.ok(tasks);
    }

    // ===================================================================
    // RAPPORTER OCH STATISTIK ENDPOINTS
    // ===================================================================

    /**
     * GET /api/tasks/statistics/monthly - Genererar månadsstatistik
     *
     * @param month Månad att generera statistik för (YYYY-MM format)
     * @return HTTP 200 OK med statistikdata
     */
    @GetMapping("/statistics/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyStatistics(
            @RequestParam(required = false) String month) {

        YearMonth targetMonth = month != null ? YearMonth.parse(month) : YearMonth.now();
        Map<String, Object> statistics = taskService.generateMonthlyTaskStatistics(targetMonth);

        return ResponseEntity.ok(statistics);
    }

    /**
     * GET /api/tasks/statistics/status-summary - Hämtar statussammanfattning
     *
     * @return HTTP 200 OK med översikt över alla statusar
     */
    @GetMapping("/statistics/status-summary")
    public ResponseEntity<Map<String, Object>> getStatusSummary() {
        Map<String, Object> summary = Map.of(
                "availableStatuses", TaskStatusUtils.getStatusSummary(),
                "statusDisplayNames", TaskStatusUtils.getAllStatuses().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                TaskStatus::name,
                                TaskStatusUtils::getDisplayName
                        ))
        );

        return ResponseEntity.ok(summary);
    }

    // ===================================================================
    // METADATA OCH HJÄLP ENDPOINTS
    // ===================================================================

    /**
     * GET /api/tasks/{id}/allowed-transitions - Hämtar tillåtna statusövergångar
     *
     * Detta är en hjälpfunktion för frontend som behöver veta vilka statusändringar
     * som är tillåtna för att visa rätt användargränssnittsalternativ.
     *
     * @param id Uppdragets ID
     * @return HTTP 200 OK med lista av tillåtna statusövergångar
     */
    @GetMapping("/{id}/allowed-transitions")
    public ResponseEntity<Map<String, Object>> getAllowedTransitions(@PathVariable Long id) {
        TaskResponseDto task = taskService.getTaskById(id);

        Set<TaskStatus> allowedTransitions = TaskStatusUtils.getValidTransitions(task.getStatus());
        Map<String, String> transitionDisplayNames = allowedTransitions.stream()
                .collect(java.util.stream.Collectors.toMap(
                        TaskStatus::name,
                        TaskStatusUtils::getDisplayName
                ));

        Map<String, Object> response = Map.of(
                "currentStatus", task.getStatus().name(),
                "currentStatusDisplay", TaskStatusUtils.getDisplayName(task.getStatus()),
                "allowedTransitions", allowedTransitions.stream()
                        .map(TaskStatus::name)
                        .collect(java.util.stream.Collectors.toSet()),
                "transitionDisplayNames", transitionDisplayNames
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/tasks/help/status-transitions - Hämtar hjälpinformation om statusövergångar
     *
     * @return HTTP 200 OK med komplett information om state machine
     */
    @GetMapping("/help/status-transitions")
    public ResponseEntity<Map<String, Object>> getStatusTransitionHelp() {
        Map<String, Set<String>> allTransitions = TaskStatusUtils.getAllStatuses().stream()
                .collect(java.util.stream.Collectors.toMap(
                        TaskStatus::name,
                        status -> TaskStatusUtils.getValidTransitions(status).stream()
                                .map(TaskStatus::name)
                                .collect(java.util.stream.Collectors.toSet())
                ));

        Map<String, Object> help = Map.of(
                "statusDescriptions", TaskStatusUtils.getStatusSummary(),
                "validTransitions", allTransitions,
                "businessRules", Map.of(
                        "ACTIVE", "Uppdrag som kan ta emot ny arbetstidsregistrering",
                        "COMPLETED", "Avslutade uppdrag - endast övergång till CANCELLED möjlig",
                        "CANCELLED", "Slutgiltigt tillstånd - inga övergångar tillåtna"
                )
        );

        return ResponseEntity.ok(help);
    }

    // ===================================================================
    // EXCEPTION HANDLERS
    // ===================================================================

    /**
     * Hanterar TaskNotFoundException - mappar till HTTP 404 Not Found
     */
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTaskNotFound(TaskNotFoundException ex) {
        Map<String, Object> errorResponse = Map.of(
                "error", "Task not found",
                "message", ex.getMessage(),
                "searchCriteria", ex.getSearchCriteria() != null ? ex.getSearchCriteria() : "",
                "searchType", ex.getSearchType() != null ? ex.getSearchType() : "",
                "userFriendlyExplanation", ex.getUserFriendlyExplanation()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Hanterar InvalidTaskTransitionException - mappar till HTTP 409 Conflict
     */
    @ExceptionHandler(InvalidTaskTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(InvalidTaskTransitionException ex) {
        Map<String, Object> errorResponse = Map.of(
                "error", "Invalid status transition",
                "message", ex.getMessage(),
                "taskId", ex.getTaskId() != null ? ex.getTaskId() : "",
                "taskNumber", ex.getTaskNumber() != null ? ex.getTaskNumber() : "",
                "currentStatus", ex.getCurrentStatus() != null ? ex.getCurrentStatus().name() : "",
                "attemptedStatus", ex.getAttemptedStatus() != null ? ex.getAttemptedStatus().name() : "",
                "allowedTransitions", ex.getAllowedTransitions() != null ?
                        ex.getAllowedTransitions().stream().map(TaskStatus::name).collect(java.util.stream.Collectors.toSet()) :
                        Set.of(),
                "explanation", ex.getUserFriendlyExplanation(),
                "suggestedActions", ex.getSuggestedActions(),
                "userResolvable", ex.isUserResolvable()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Hanterar TaskDeletionException - mappar till HTTP 409 Conflict
     */
    @ExceptionHandler(TaskDeletionException.class)
    public ResponseEntity<Map<String, Object>> handleTaskDeletionError(TaskDeletionException ex) {
        Map<String, Object> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Task deletion not allowed");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("taskId", ex.getTaskId() != null ? ex.getTaskId() : "");
        errorResponse.put("taskNumber", ex.getTaskNumber() != null ? ex.getTaskNumber() : "");
        errorResponse.put("blockerType", ex.getBlockerType() != null ? ex.getBlockerType().name() : "");
        errorResponse.put("relatedEntitiesCount", ex.getRelatedEntitiesCount());
        errorResponse.put("explanation", ex.getUserFriendlyExplanation());
        errorResponse.put("suggestedActions", ex.getSuggestedActions());
        errorResponse.put("userResolvable", ex.isUserResolvable());
        errorResponse.put("mayBecomeResolvable", ex.mayBecomeResolvable());
        errorResponse.put("resolutionTimeframe", ex.getResolutionTimeframe());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Hanterar DuplicateTaskException - mappar till HTTP 409 Conflict
     */
    @ExceptionHandler(DuplicateTaskException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateTask(DuplicateTaskException ex) {
        Map<String, Object> errorResponse = Map.of(
                "error", "Duplicate task",
                "message", ex.getMessage(),
                "suggestion", "Välj ett annat uppdragsnummer eller kontrollera om uppdraget redan finns"
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Hanterar generella IllegalArgumentException - mappar till HTTP 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> errorResponse = Map.of(
                "error", "Invalid request",
                "message", ex.getMessage()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Hanterar alla andra fel - mappar till HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        Map<String, Object> errorResponse = Map.of(
                "error", "Internal server error",
                "message", "Ett oväntat fel uppstod. Kontakta systemadministratör."
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}