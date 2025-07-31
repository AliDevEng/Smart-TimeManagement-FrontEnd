package com.gardening.timemanagement.service;

import com.gardening.timemanagement.util.ValidationResult;
import com.gardening.timemanagement.dto.request.CreateWorkDayDto;
import com.gardening.timemanagement.dto.request.UpdateWorkDayDto;
import com.gardening.timemanagement.dto.response.WorkDayResponseDto;
import com.gardening.timemanagement.entity.*;
import com.gardening.timemanagement.exception.*;
import com.gardening.timemanagement.mapper.WorkDayMapper;
import com.gardening.timemanagement.repository.*;
import com.gardening.timemanagement.util.WorkDayValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true) // Default för performance optimization
public class WorkDayService {

    // Repository dependencies för data access
    private final WorkDayRepository workDayRepository;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final EquipmentRepository equipmentRepository;

    // Service dependencies för business logic coordination
    private final TaskService taskService;
    private final EmployeeService employeeService;

    // Mapper för DTO transformations
    private final WorkDayMapper workDayMapper;

    // Business rule constants - configurable i production systems
    private static final int MAX_CONCURRENT_WORKDAYS_PER_EMPLOYEE = 1;
    private static final int MAX_RETROACTIVE_DAYS = 30;
    private static final int MAX_FUTURE_PLANNING_DAYS = 90;
    private static final double MAX_DAILY_HOURS_PER_EMPLOYEE = 16.0;
    private static final double MAX_TOTAL_DAILY_HOURS = 200.0;

    public WorkDayService(WorkDayRepository workDayRepository,
                          TaskRepository taskRepository,
                          EmployeeRepository employeeRepository,
                          EquipmentRepository equipmentRepository,
                          TaskService taskService,
                          EmployeeService employeeService,
                          WorkDayMapper workDayMapper) {
        this.workDayRepository = workDayRepository;
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
        this.equipmentRepository = equipmentRepository;
        this.taskService = taskService;
        this.employeeService = employeeService;
        this.workDayMapper = workDayMapper;
    }

    // =================================================================
    // CREATE OPERATIONS - COMPLEX BUSINESS PROCESS ORCHESTRATION
    // =================================================================

    /**
     * Skapar en ny WorkDay med comprehensive business rule validation.
     *
     * Detta är den mest kritiska metoden i service eftersom den orchestrates
     * creation av en complex aggregate root med multiple related entities.
     * All validation måste succeed och all data måste be consistent innan
     * any persistence occurs.
     *
     * @param createDto Validated input data från client
     * @return Newly created WorkDay med full related data
     * @throws InvalidWorkDayException om business rules inte kan satisfied
     * @throws DuplicateWorkDayException om conflicting WorkDay redan exists
     */
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class,
            timeout = 30
    )
    public WorkDayResponseDto createWorkDay(CreateWorkDayDto createDto) {
        // Phase 1: Pre-creation validation - fail fast för obvious problems
        validateCreateWorkDayRequest(createDto);

        // Phase 2: Business rule validation - comprehensive cross-entity checks
        validateBusinessRulesForCreation(createDto);

        // Phase 3: Resource availability validation - ensure no conflicts
        validateResourceAvailabilityForCreation(createDto);

        // Phase 4: Entity creation med transactional safety
        WorkDay workDay = createWorkDayEntity(createDto);

        // Phase 5: Post-creation processing - audit, notifications, integration
        processPostCreationActivities(workDay);

        // Phase 6: Response generation med optimal expansion
        return workDayMapper.toResponseDto(workDay, WorkDayMapper.EXPANSION_DETAILED);
    }

    /**
     * Validates basic request structure och field-level constraints.
     *
     * Denna metod performs fast validation som can reject obviously
     * invalid requests without expensive database operations.
     */
    private void validateCreateWorkDayRequest(CreateWorkDayDto createDto) {
        if (createDto == null) {
            throw new IllegalArgumentException("CreateWorkDayDto kan inte vara null");
        }

        // Validate using our sophisticated validation utils
        WorkDayValidationUtils.ValidationResult dateResult =
                WorkDayValidationUtils.validateWorkDayDate(createDto.getDate(), false);

        if (!dateResult.isValid()) {
            throw new InvalidWorkDayException("Ogiltigt datum",
                    dateResult.getMessage().orElse("Okänt datumfel"));
        }

        // Validate employee time entries på DTO level
        if (createDto.getEmployeeTimes() == null || createDto.getEmployeeTimes().isEmpty()) {
            throw new InvalidWorkDayException("Arbetstider saknas",
                    "Minst en medarbetares arbetstid måste registreras");
        }

        // Check för duplicate employees i input
        if (createDto.hasDuplicateEmployees()) {
            throw new InvalidWorkDayException("Dubletter av medarbetare",
                    "Samma medarbetare kan inte registreras flera gånger för samma arbetsdag");
        }

        // Validate equipment duplicates
        if (createDto.hasDuplicateEquipment()) {
            throw new InvalidWorkDayException("Dubletter av utrustning",
                    "Samma utrustning kan inte registreras flera gånger för samma arbetsdag");
        }
    }

    /**
     * Validates complex business rules som require cross-entity coordination.
     *
     * Denna metod performs expensive validation som involves database
     * queries och complex business logic evaluation.
     */
    private void validateBusinessRulesForCreation(CreateWorkDayDto createDto) {
        // Validate task suitability och availability
        Task task = taskRepository.findById(createDto.getTaskId())
                .orElseThrow(() -> new InvalidWorkDayException("Task inte hittad",
                        "Uppdraget med ID " + createDto.getTaskId() + " finns inte"));

        WorkDayValidationUtils.ValidationResult taskResult =
                WorkDayValidationUtils.validateTaskSuitability(task, createDto.getDate());

        if (!taskResult.isValid()) {
            throw new InvalidWorkDayException("Task ej lämplig",
                    taskResult.getMessage().orElse("Task kan inte användas för denna arbetsdag"));
        }

        // Validate all employees är suitable för assignment
        List<Employee> employees = resolveEmployeesFromDto(createDto);
        List<WorkDay> existingWorkDays = workDayRepository.findByDate(createDto.getDate());

        WorkDayValidationUtils.ValidationResult employeeResult =
                WorkDayValidationUtils.validateEmployeeAssignments(employees, createDto.getDate(), existingWorkDays);

        if (!employeeResult.isValid()) {
            throw new InvalidWorkDayException("Medarbetarproblem",
                    employeeResult.getMessage().orElse("Ett eller flera medarbetarproblem upptäcktes"));
        }

        // Validate supervisor if specified
        if (createDto.getSupervisorId() != null) {
            Employee supervisor = employeeRepository.findById(createDto.getSupervisorId())
                    .orElseThrow(() -> new InvalidWorkDayException("Arbetsledare inte hittad",
                            "Arbetsledaren med ID " + createDto.getSupervisorId() + " finns inte"));

            if (!supervisor.canBeAssignedToWork()) {
                throw new InvalidWorkDayException("Inaktiv arbetsledare",
                        "Arbetsledaren '" + supervisor.getName() + "' är inaktiv och kan inte tilldelas");
            }
        }

        // Validate equipment availability
        if (createDto.getEquipmentUsage() != null && !createDto.getEquipmentUsage().isEmpty()) {
            validateEquipmentAvailabilityForCreation(createDto);
        }
    }

    /**
     * Validates att resources är available och inte conflicting.
     */
    private void validateResourceAvailabilityForCreation(CreateWorkDayDto createDto) {
        // Check för existing WorkDay för samma task och date
        boolean workDayExists = workDayRepository.existsByTaskIdAndDate(
                createDto.getTaskId(), createDto.getDate());

        if (workDayExists) {
            throw new DuplicateWorkDayException(
                    "En arbetsdag för detta uppdrag och datum finns redan",
                    createDto.getTaskId(), createDto.getDate());
        }

        // Check för employee conflicts på samma datum
        for (CreateWorkDayDto.EmployeeTimeDto employeeTimeDto : createDto.getEmployeeTimes()) {
            boolean employeeAlreadyWorking = employeeRepository.isEmployeeRegisteredForWorkDay(
                    employeeTimeDto.getEmployeeId(), createDto.getDate(), createDto.getTaskId());

            if (employeeAlreadyWorking) {
                Employee employee = employeeRepository.findById(employeeTimeDto.getEmployeeId())
                        .orElseThrow(() -> new InvalidWorkDayException("Employee inte hittad",
                                "Medarbetare med ID " + employeeTimeDto.getEmployeeId() + " finns inte"));

                throw new InvalidWorkDayException("Medarbetarkonflikt",
                        "Medarbetaren '" + employee.getName() + "' är redan registrerad för arbete på " + createDto.getDate());
            }
        }
    }

    /**
     * Creates WorkDay entity med all related entities inom transactional context.
     */
    private WorkDay createWorkDayEntity(CreateWorkDayDto createDto) {
        try {
            // Transform DTO till entity using sophisticated mapper
            WorkDay workDay = workDayMapper.toEntity(createDto);

            // Additional business logic setup
            enrichWorkDayForCreation(workDay);

            // Persist med cascade för related entities
            WorkDay savedWorkDay = workDayRepository.save(workDay);

            // Validate post-persistence state
            validatePostCreationState(savedWorkDay);

            return savedWorkDay;

        } catch (Exception e) {
            // Wrap any unexpected errors i business exception
            throw new InvalidWorkDayException("Skapande misslyckades",
                    "Ett oväntat fel uppstod vid skapande av arbetstag: " + e.getMessage(), e);
        }
    }

    /**
     * Enriches WorkDay med computed fields och business logic.
     */
    private void enrichWorkDayForCreation(WorkDay workDay) {
        // Validate aggregated work time is reasonable
        double totalHours = workDay.getTotalWorkHours();
        if (totalHours > MAX_TOTAL_DAILY_HOURS) {
            throw new InvalidWorkDayException("För många arbetstimmar",
                    String.format("Total arbetstid %.1f timmar överstiger maximum %.1f timmar per dag",
                            totalHours, MAX_TOTAL_DAILY_HOURS));
        }

        // Validate individual employee work times
        for (EmployeeTime employeeTime : workDay.getEmployeeTimes()) {
            double individualHours = employeeTime.getTotalHours().doubleValue();
            if (individualHours > MAX_DAILY_HOURS_PER_EMPLOYEE) {
                throw new InvalidWorkDayException("För många timmar per medarbetare",
                        String.format("Medarbetaren '%s' har %.1f timmar vilket överstiger maximum %.1f timmar per dag",
                                employeeTime.getEmployee().getName(), individualHours, MAX_DAILY_HOURS_PER_EMPLOYEE));
            }

            // Validate work time entry using comprehensive validation
            employeeService.validateWorkTimeEntry(employeeTime);
        }
    }

    /**
     * Validates att persisted state är consistent med business rules.
     */
    private void validatePostCreationState(WorkDay savedWorkDay) {
        // Comprehensive validation using our validation utils
        List<WorkDay> existingWorkDays = workDayRepository.findByDate(savedWorkDay.getDate());
        List<WorkDayEquipment> existingEquipment = workDayRepository.findByDate(savedWorkDay.getDate())
                .stream()
                .flatMap(wd -> wd.getEquipmentUsed().stream())
                .collect(Collectors.toList());

        WorkDayValidationUtils.ComprehensiveValidationResult validationResult =
                WorkDayValidationUtils.validateCompleteWorkDay(
                        savedWorkDay, false, existingWorkDays, existingEquipment);

        if (!validationResult.isValid()) {
            throw new InvalidWorkDayException("Post-creation validation misslyckades",
                    "Arbetsdagen kunde skapas men uppfyller inte alla business rules: " +
                            String.join("; ", validationResult.getErrorMessages()));
        }
    }

    /**
     * Processes post-creation activities som audit logging och notifications.
     */
    private void processPostCreationActivities(WorkDay workDay) {
        try {
            // Audit logging för compliance
            logWorkDayCreation(workDay);

            // Business event publication för integration
            publishWorkDayCreatedEvent(workDay);

            // Update related entity statistics
            updateTaskStatistics(workDay.getTask());

        } catch (Exception e) {
            // Log errors men don't fail entire operation för non-critical activities
            logNonCriticalError("Post-creation processing", e);
        }
    }

    // =================================================================
    // READ OPERATIONS - OPTIMIZED QUERY STRATEGIES
    // =================================================================

    /**
     * Hämtar WorkDay by ID med intelligent expansion baserat på usage context.
     *
     * @param id WorkDay ID
     * @param expansionHint Optional hint för desired expansion level
     * @return WorkDay response DTO eller null om inte found
     */
    public Optional<WorkDayResponseDto> getWorkDayById(Long id, String expansionHint) {
        if (id == null) {
            return Optional.empty();
        }

        Optional<WorkDay> workDayOpt = workDayRepository.findById(id);
        if (workDayOpt.isEmpty()) {
            return Optional.empty();
        }

        WorkDay workDay = workDayOpt.get();

        // Use provided expansion hint eller determine automatically
        String expansionLevel = expansionHint != null ? expansionHint :
                determineOptimalExpansionForSingleEntity(workDay);

        WorkDayResponseDto responseDto = workDayMapper.toResponseDto(workDay, expansionLevel);
        return Optional.of(responseDto);
    }

    /**
     * Hämtar WorkDays för specific date med performance optimization.
     *
     * @param date Target date
     * @param includeDetails Whether att include detailed information
     * @return List av WorkDay responses
     */
    public List<WorkDayResponseDto> getWorkDaysByDate(LocalDate date, boolean includeDetails) {
        if (date == null) {
            throw new IllegalArgumentException("Date kan inte vara null");
        }

        List<WorkDay> workDays = workDayRepository.findByDate(date);

        String expansionLevel = includeDetails ?
                WorkDayMapper.EXPANSION_DETAILED : WorkDayMapper.EXPANSION_SUMMARY;

        return workDayMapper.toResponseDtoList(workDays, expansionLevel);
    }

    /**
     * Hämtar WorkDays för specific task med comprehensive data.
     *
     * @param taskId Task ID
     * @param includeMetrics Whether att include business metrics
     * @return List av WorkDay responses för the task
     */
    public List<WorkDayResponseDto> getWorkDaysByTask(Long taskId, boolean includeMetrics) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID kan inte vara null");
        }

        // Validate that task exists
        if (!taskRepository.existsById(taskId)) {
            throw new IllegalArgumentException("Task med ID " + taskId + " finns inte");
        }

        List<WorkDay> workDays = workDayRepository.findByTaskId(taskId);

        String expansionLevel = includeMetrics ?
                WorkDayMapper.EXPANSION_FULL : WorkDayMapper.EXPANSION_DETAILED;

        return workDayMapper.toResponseDtoList(workDays, expansionLevel);
    }

    /**
     * Söker WorkDays within date range med advanced filtering options.
     *
     * @param startDate Start av date range (inclusive)
     * @param endDate End av date range (inclusive)
     * @param includeMetrics Whether att include business intelligence metrics
     * @return List av matching WorkDay responses
     */
    public List<WorkDayResponseDto> getWorkDaysInDateRange(LocalDate startDate, LocalDate endDate,
                                                           boolean includeMetrics) {
        // Validate date range using our validation utils
        WorkDayValidationUtils.ValidationResult dateRangeResult =
                WorkDayValidationUtils.validateReportDateRange(startDate, endDate);

        if (!dateRangeResult.isValid()) {
            throw new IllegalArgumentException("Ogiltigt datumintervall: " +
                    dateRangeResult.getMessage().orElse("Okänt datumfel"));
        }

        List<WorkDay> workDays = workDayRepository.findByDateBetween(startDate, endDate);

        // Use performance-optimized expansion för large result sets
        String expansionLevel = workDays.size() > 20 ?
                WorkDayMapper.EXPANSION_SUMMARY :
                (includeMetrics ? WorkDayMapper.EXPANSION_DETAILED : WorkDayMapper.EXPANSION_SUMMARY);

        return workDayMapper.toResponseDtoList(workDays, expansionLevel);
    }

    // =================================================================
    // UPDATE OPERATIONS - SOPHISTICATED STATE TRANSITION MANAGEMENT
    // =================================================================


    @Transactional(
            isolation = Isolation.REPEATABLE_READ,
            rollbackFor = Exception.class,
            timeout = 45
    )
    public WorkDayResponseDto updateWorkDay(Long workDayId, UpdateWorkDayDto updateDto) {
        // Phase 1: Entity retrieval med existence validation
        WorkDay existingWorkDay = retrieveWorkDayForUpdate(workDayId);

        // Phase 2: Pre-update validation - version control och basic checks
        validateUpdateRequest(existingWorkDay, updateDto);

        // Phase 3: Business rule validation för proposed changes
        validateBusinessRulesForUpdate(existingWorkDay, updateDto);

        // Phase 4: State transition execution med careful error handling
        WorkDay updatedWorkDay = executeWorkDayUpdate(existingWorkDay, updateDto);

        // Phase 5: Post-update processing - audit, notifications, consistency checks
        processPostUpdateActivities(existingWorkDay, updatedWorkDay, updateDto);

        // Phase 6: Response generation med appropriate expansion
        return workDayMapper.toResponseDto(updatedWorkDay, WorkDayMapper.EXPANSION_EDIT);
    }

    /**
     * Retrieves WorkDay för update med proper locking strategy.
     */
    private WorkDay retrieveWorkDayForUpdate(Long workDayId) {
        if (workDayId == null) {
            throw new IllegalArgumentException("WorkDay ID kan inte vara null");
        }

        return workDayRepository.findById(workDayId)
                .orElseThrow(() -> new WorkDayNotFoundException(
                        "WorkDay med ID " + workDayId + " finns inte"));
    }

    /**
     * Validates update request structure och version control.
     */
    private void validateUpdateRequest(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        if (updateDto == null) {
            throw new IllegalArgumentException("UpdateWorkDayDto kan inte vara null");
        }

        // Check om WorkDay can be updated enligt business rules
        if (!isWorkDayEditable(existingWorkDay)) {
            throw new InvalidWorkDayTransitionException(
                    "Arbetsdagen kan inte längre redigeras",
                    "WorkDay från " + existingWorkDay.getDate() + " kan inte redigeras enligt företagspolicy");
        }

        // Validate destructive operations have proper authorization
        if (updateDto.hasDestructiveOperations() && !updateDto.isConfirmDestructiveChanges()) {
            throw new InvalidWorkDayTransitionException(
                    "Destructive changes kräver bekräftelse",
                    "Denna uppdatering innehåller destructive operations som kräver explicit bekräftelse");
        }

        // Validate that user understands consequences av override flags
        if (updateDto.isOverrideBusinessRules() || updateDto.isForceConcurrentUpdate()) {
            if (!updateDto.isConfirmDestructiveChanges()) {
                throw new InvalidWorkDayTransitionException(
                        "Override operations kräver bekräftelse",
                        "Business rule overrides kräver explicit bekräftelse av consequences");
            }
        }
    }

    /**
     * Validates business rules för proposed update changes.
     */
    private void validateBusinessRulesForUpdate(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        // Validate date changes if specified
        if (updateDto.getDate() != null && !updateDto.getDate().equals(existingWorkDay.getDate())) {
            validateDateChangeBusinessRules(existingWorkDay, updateDto.getDate());
        }

        // Validate task changes if specified
        if (updateDto.getTaskId() != null && !updateDto.getTaskId().equals(existingWorkDay.getTask().getId())) {
            validateTaskChangeBusinessRules(existingWorkDay, updateDto.getTaskId());
        }

        // Validate employee time changes enligt strategy
        if (updateDto.getEmployeeTimeStrategy() != UpdateWorkDayDto.CollectionUpdateStrategy.NO_CHANGE) {
            validateEmployeeTimeUpdateBusinessRules(existingWorkDay, updateDto);
        }

        // Validate equipment changes enligt strategy
        if (updateDto.getEquipmentStrategy() != UpdateWorkDayDto.CollectionUpdateStrategy.NO_CHANGE) {
            validateEquipmentUpdateBusinessRules(existingWorkDay, updateDto);
        }
    }

    /**
     * Executes the actual update using sophisticated mapper coordination.
     */
    private WorkDay executeWorkDayUpdate(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        try {
            // Use mapper för sophisticated update logic
            WorkDay updatedWorkDay = workDayMapper.updateEntityFromDto(existingWorkDay, updateDto);

            // Additional business logic enrichment efter update
            enrichWorkDayForUpdate(updatedWorkDay, updateDto);

            // Persist changes med optimistic locking protection
            WorkDay savedWorkDay = workDayRepository.save(updatedWorkDay);

            // Validate post-update consistency
            validatePostUpdateConsistency(savedWorkDay);

            return savedWorkDay;

        } catch (Exception e) {
            // Comprehensive error handling med context preservation
            handleUpdateExecutionError(existingWorkDay, updateDto, e);
            throw e; // Re-throw efter logging
        }
    }

    // =================================================================
    // DELETE OPERATIONS - SAFE REMOVAL MED BUSINESS RULE ENFORCEMENT
    // =================================================================

    /**
     * Safely removes WorkDay med comprehensive validation och cleanup.
     *
     * Deletion av WorkDay är potentially destructive eftersom det affects
     * payroll calculations, project reports, och audit trails. This operation
     * requires careful validation och often should be restricted eller
     * replaced med "soft delete" patterns.
     *
     * @param workDayId ID av WorkDay att remove
     * @param forceDelete Whether att bypass certain safety checks
     * @throws WorkDayDeletionException om deletion violates business rules
     */
    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            rollbackFor = Exception.class,
            timeout = 30
    )
    public void deleteWorkDay(Long workDayId, boolean forceDelete) {
        // Phase 1: Retrieve WorkDay med full related data
        WorkDay workDay = retrieveWorkDayForDeletion(workDayId);

        // Phase 2: Validate deletion är safe enligt business rules
        validateWorkDayDeletionSafety(workDay, forceDelete);

        // Phase 3: Pre-deletion cleanup och preparation
        prepareWorkDayForDeletion(workDay);

        // Phase 4: Execute deletion med cascade handling
        executeWorkDayDeletion(workDay);

        // Phase 5: Post-deletion cleanup och audit logging
        processPostDeletionActivities(workDay);
    }

    /**
     * Retrieves WorkDay för deletion med all related entities.
     */
    private WorkDay retrieveWorkDayForDeletion(Long workDayId) {
        if (workDayId == null) {
            throw new IllegalArgumentException("WorkDay ID kan inte vara null");
        }

        // Use detailed fetch för complete deletion validation
        Optional<WorkDay> workDayOpt = workDayRepository.findById(workDayId);
        if (workDayOpt.isEmpty()) {
            throw new WorkDayNotFoundException("WorkDay med ID " + workDayId + " finns inte för deletion");
        }

        return workDayOpt.get();
    }

    /**
     * Validates att deletion är safe och legal enligt business rules.
     */
    private void validateWorkDayDeletionSafety(WorkDay workDay, boolean forceDelete) {
        // Check temporal constraints - cannot delete old WorkDays without special authorization
        if (!forceDelete) {
            LocalDate cutoffDate = LocalDate.now().minusDays(MAX_RETROACTIVE_DAYS);
            if (workDay.getDate().isBefore(cutoffDate)) {
                throw new WorkDayDeletionException(
                        "WorkDay för gammal för deletion",
                        String.format("WorkDays äldre än %d dagar kan inte tas bort utan special authorization",
                                MAX_RETROACTIVE_DAYS));
            }
        }

        // Check om WorkDay has been processed by payroll systems
        if (hasBeenProcessedByPayroll(workDay) && !forceDelete) {
            throw new WorkDayDeletionException(
                    "WorkDay redan processed av payroll",
                    "Denna arbetsdag har redan bearbetats av lönesystemet och kan inte tas bort");
        }

        // Check för related audit requirements
        if (hasActiveAuditTrail(workDay) && !forceDelete) {
            throw new WorkDayDeletionException(
                    "Active audit trail prevents deletion",
                    "Denna arbetsdag är subject till active audit och kan inte tas bort");
        }

        // Check för project reporting dependencies
        if (isRequiredForProjectReporting(workDay) && !forceDelete) {
            throw new WorkDayDeletionException(
                    "Required för project reporting",
                    "Denna arbetsdag är required för pågående project reporting och kan inte tas bort");
        }
    }

    /**
     * Prepares WorkDay för safe deletion med cleanup activities.
     */
    private void prepareWorkDayForDeletion(WorkDay workDay) {
        // Archive WorkDay data för audit trail preservation
        archiveWorkDayForAudit(workDay);

        // Notify related systems av impending deletion
        notifySystemsOfPendingDeletion(workDay);

        // Update dependent statistics och summaries
        updateDependentStatisticsForDeletion(workDay);
    }

    /**
     * Executes WorkDay deletion med proper cascade handling.
     */
    private void executeWorkDayDeletion(WorkDay workDay) {
        try {
            // JPA cascade should handle related entities, men we validate explicitly
            validateCascadeDeletionSafety(workDay);

            // Perform actual deletion
            workDayRepository.delete(workDay);

            // Explicit flush för immediate constraint validation
            workDayRepository.flush();

        } catch (Exception e) {
            throw new WorkDayDeletionException(
                    "Deletion execution failed",
                    "Ett tekniskt fel uppstod vid borttagning av arbetsdagen: " + e.getMessage(), e);
        }
    }

    /**
     * Processes post-deletion activities för consistency maintenance.
     */
    private void processPostDeletionActivities(WorkDay workDay) {
        try {
            // Audit logging för compliance
            logWorkDayDeletion(workDay);

            // Publish deletion event för system integration
            publishWorkDayDeletedEvent(workDay);

            // Update cached statistics och summaries
            refreshRelatedStatistics(workDay);

        } catch (Exception e) {
            // Log errors men don't fail entire operation för non-critical activities
            logNonCriticalError("Post-deletion processing", e);
        }
    }

    // =================================================================
    // BUSINESS INTELLIGENCE OCH REPORTING METHODS
    // =================================================================

    /**
     * Generates comprehensive monthly report för specified month.
     *
     * Denna metod aggregates WorkDay data över en month och produces
     * detailed business intelligence för management reporting.
     *
     * @param year Target year
     * @param month Target month (1-12)
     * @return Comprehensive monthly report med metrics och analysis
     */
    public MonthlyWorkDayReportDto generateMonthlyReport(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month måste vara mellan 1 och 12");
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        // Validate report date range
        WorkDayValidationUtils.ValidationResult dateRangeResult =
                WorkDayValidationUtils.validateReportDateRange(startDate, endDate);

        if (!dateRangeResult.isValid()) {
            throw new IllegalArgumentException("Ogiltigt rapportintervall: " +
                    dateRangeResult.getMessage().orElse("Okänt datumfel"));
        }

        // Gather comprehensive data för report generation
        List<WorkDay> monthWorkDays = workDayRepository.findByDateBetween(startDate, endDate);

        return generateComprehensiveMonthlyReport(monthWorkDays, year, month);
    }

    /**
     * Calculates productivity metrics för specified employee över date range.
     *
     * @param employeeId Target employee
     * @param startDate Start av analysis period
     * @param endDate End av analysis period
     * @return Detailed productivity analysis
     */
    public EmployeeProductivityReportDto analyzeEmployeeProductivity(Long employeeId,
                                                                     LocalDate startDate,
                                                                     LocalDate endDate) {
        // Validate employee exists
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee med ID " + employeeId + " finns inte"));

        // Validate date range
        WorkDayValidationUtils.ValidationResult dateRangeResult =
                WorkDayValidationUtils.validateReportDateRange(startDate, endDate);

        if (!dateRangeResult.isValid()) {
            throw new IllegalArgumentException("Ogiltigt datumintervall: " +
                    dateRangeResult.getMessage().orElse("Okänt datumfel"));
        }

        // Gather employee work data
        List<WorkDay> employeeWorkDays = workDayRepository.findWorkDaysForEmployee(employeeId)
                .stream()
                .filter(wd -> !wd.getDate().isBefore(startDate) && !wd.getDate().isAfter(endDate))
                .collect(Collectors.toList());

        return analyzeEmployeeProductivityFromWorkDays(employee, employeeWorkDays, startDate, endDate);
    }

    // =================================================================
    // HELPER METHODS - BUSINESS LOGIC SUPPORT
    // =================================================================

    /**
     * Resolves Employee entities från CreateWorkDayDto input.
     */
    private List<Employee> resolveEmployeesFromDto(CreateWorkDayDto createDto) {
        return createDto.getEmployeeTimes().stream()
                .map(employeeTimeDto -> {
                    return employeeRepository.findById(employeeTimeDto.getEmployeeId())
                            .orElseThrow(() -> new InvalidWorkDayException("Employee inte hittad",
                                    "Medarbetare med ID " + employeeTimeDto.getEmployeeId() + " finns inte"));
                })
                .collect(Collectors.toList());
    }

    /**
     * Validates equipment availability för creation scenario.
     */
    private void validateEquipmentAvailabilityForCreation(CreateWorkDayDto createDto) {
        for (CreateWorkDayDto.EquipmentUsageDto equipmentDto : createDto.getEquipmentUsage()) {
            Equipment equipment = equipmentRepository.findById(equipmentDto.getEquipmentId())
                    .orElseThrow(() -> new InvalidWorkDayException("Equipment inte hittad",
                            "Utrustningen med ID " + equipmentDto.getEquipmentId() + " finns inte"));

            if (!equipment.isAvailableForBooking()) {
                throw new InvalidWorkDayException("Utrustning inte tillgänglig",
                        "Utrustningen '" + equipment.getName() + "' är inte tillgänglig för bokning");
            }

            // Check för potential conflicts (simplified - real system skulle be more sophisticated)
            boolean equipmentAlreadyBooked = equipmentRepository.isEquipmentUsedOnDate(
                    equipmentDto.getEquipmentId(), createDto.getDate());

            if (equipmentAlreadyBooked) {
                throw new InvalidWorkDayException("Utrustning redan bokad",
                        "Utrustningen '" + equipment.getName() + "' är redan bokad för " + createDto.getDate());
            }
        }
    }

    /**
     * Determines optimal expansion level för single entity retrieval.
     */
    private String determineOptimalExpansionForSingleEntity(WorkDay workDay) {
        // För single entity requests kan vi afford more detailed expansion
        if (workDay.getEmployeeTimes().size() <= 5 && workDay.getEquipmentUsed().size() <= 5) {
            return WorkDayMapper.EXPANSION_FULL;
        } else {
            return WorkDayMapper.EXPANSION_DETAILED;
        }
    }

    /**
     * Checks om WorkDay can be edited enligt business rules.
     */
    private boolean isWorkDayEditable(WorkDay workDay) {
        // Cannot edit WorkDays older than policy allows
        LocalDate cutoffDate = LocalDate.now().minusDays(MAX_RETROACTIVE_DAYS);
        if (workDay.getDate().isBefore(cutoffDate)) {
            return false;
        }

        // Cannot edit WorkDays för inactive tasks
        if (workDay.getTask() != null && !workDay.getTask().canAcceptWorkTime()) {
            return false;
        }

        // Cannot edit WorkDays som has been processed by payroll
        if (hasBeenProcessedByPayroll(workDay)) {
            return false;
        }

        return true;
    }

    // =================================================================
    // BUSINESS RULE VALIDATION METHODS FÖR UPDATES
    // =================================================================

    /**
     * Validates business rules för date change operations.
     */
    private void validateDateChangeBusinessRules(WorkDay existingWorkDay, LocalDate newDate) {
        // Validate new date enligt temporal business rules
        WorkDayValidationUtils.ValidationResult dateResult =
                WorkDayValidationUtils.validateWorkDayDate(newDate, false);

        if (!dateResult.isValid()) {
            throw new InvalidWorkDayTransitionException("Ogiltigt nytt datum",
                    dateResult.getMessage().orElse("Det nya datumet är inte giltigt"));
        }

        // Check för conflicts på new date
        boolean conflictExists = workDayRepository.existsByTaskIdAndDate(
                existingWorkDay.getTask().getId(), newDate);

        if (conflictExists) {
            throw new InvalidWorkDayTransitionException("Datum konflikt",
                    "En arbetsdag för detta uppdrag finns redan på " + newDate);
        }

        // Validate that date change doesn't violate employee availability
        for (EmployeeTime employeeTime : existingWorkDay.getEmployeeTimes()) {
            boolean employeeConflict = employeeRepository.isEmployeeRegisteredForWorkDay(
                    employeeTime.getEmployee().getId(), newDate, existingWorkDay.getTask().getId());

            if (employeeConflict) {
                throw new InvalidWorkDayTransitionException("Medarbetarkonflikt på nytt datum",
                        "Medarbetaren '" + employeeTime.getEmployee().getName() +
                                "' är redan registrerad för arbete på " + newDate);
            }
        }
    }

    /**
     * Validates business rules för task change operations.
     */
    private void validateTaskChangeBusinessRules(WorkDay existingWorkDay, Long newTaskId) {
        Task newTask = taskRepository.findById(newTaskId)
                .orElseThrow(() -> new InvalidWorkDayTransitionException("Task inte hittad",
                        "Uppdraget med ID " + newTaskId + " finns inte"));

        // Validate new task är suitable för this WorkDay
        WorkDayValidationUtils.ValidationResult taskResult =
                WorkDayValidationUtils.validateTaskSuitability(newTask, existingWorkDay.getDate());

        if (!taskResult.isValid()) {
            throw new InvalidWorkDayTransitionException("Task ej lämplig",
                    taskResult.getMessage().orElse("Det nya uppdraget är inte lämpligt för denna arbetsdag"));
        }

        // Check för conflicts med new task
        boolean conflictExists = workDayRepository.existsByTaskIdAndDate(
                newTaskId, existingWorkDay.getDate());

        if (conflictExists) {
            throw new InvalidWorkDayTransitionException("Task konflikt",
                    "En arbetsdag för det nya uppdraget finns redan på " + existingWorkDay.getDate());
        }

        // Validate that task change doesn't affect project reporting
        if (isRequiredForProjectReporting(existingWorkDay)) {
            throw new InvalidWorkDayTransitionException("Task change affects reporting",
                    "Denna arbetsdag är required för project reporting och uppdraget kan inte ändras");
        }
    }

    /**
     * Validates business rules för employee time update operations.
     */
    private void validateEmployeeTimeUpdateBusinessRules(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        // Strategy-specific validation
        switch (updateDto.getEmployeeTimeStrategy()) {
            case REPLACE_ALL:
                validateReplaceAllEmployeeTimesBusinessRules(existingWorkDay, updateDto);
                break;
            case ADD_OR_UPDATE:
                validateAddOrUpdateEmployeeTimesBusinessRules(existingWorkDay, updateDto);
                break;
            case EXPLICIT_OPERATIONS:
                validateExplicitEmployeeTimeOperationsBusinessRules(existingWorkDay, updateDto);
                break;
            default:
                // NO_CHANGE requires no validation
                break;
        }
    }

    /**
     * Validates business rules för equipment update operations.
     */
    private void validateEquipmentUpdateBusinessRules(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        // Strategy-specific validation
        switch (updateDto.getEquipmentStrategy()) {
            case REPLACE_ALL:
                validateReplaceAllEquipmentBusinessRules(existingWorkDay, updateDto);
                break;
            case ADD_OR_UPDATE:
                validateAddOrUpdateEquipmentBusinessRules(existingWorkDay, updateDto);
                break;
            case EXPLICIT_OPERATIONS:
                validateExplicitEquipmentOperationsBusinessRules(existingWorkDay, updateDto);
                break;
            default:
                // NO_CHANGE requires no validation
                break;
        }
    }

    // =================================================================
    // ENRICHMENT OCH POST-PROCESSING METHODS
    // =================================================================

    /**
     * Enriches WorkDay efter update med computed fields och validations.
     */
    private void enrichWorkDayForUpdate(WorkDay workDay, UpdateWorkDayDto updateDto) {
        // Re-validate aggregated metrics efter update
        double totalHours = workDay.getTotalWorkHours();
        if (totalHours > MAX_TOTAL_DAILY_HOURS && !updateDto.isOverrideBusinessRules()) {
            throw new InvalidWorkDayTransitionException("För många arbetstimmar efter update",
                    String.format("Total arbetstid %.1f timmar överstiger maximum %.1f timmar per dag",
                            totalHours, MAX_TOTAL_DAILY_HOURS));
        }

        // Validate individual employee work times efter update
        for (EmployeeTime employeeTime : workDay.getEmployeeTimes()) {
            double individualHours = employeeTime.getTotalHours().doubleValue();
            if (individualHours > MAX_DAILY_HOURS_PER_EMPLOYEE && !updateDto.isOverrideBusinessRules()) {
                throw new InvalidWorkDayTransitionException("För många timmar per medarbetare efter update",
                        String.format("Medarbetaren '%s' har %.1f timmar vilket överstiger maximum %.1f timmar per dag",
                                employeeTime.getEmployee().getName(), individualHours, MAX_DAILY_HOURS_PER_EMPLOYEE));
            }
        }

        // Update computed timestamps
        workDay.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Validates post-update consistency och business rule compliance.
     */
    private void validatePostUpdateConsistency(WorkDay workDay) {
        // Use comprehensive validation för updated WorkDay
        List<WorkDay> existingWorkDays = workDayRepository.findByDate(workDay.getDate())
                .stream()
                .filter(wd -> !wd.getId().equals(workDay.getId())) // Exclude self
                .collect(Collectors.toList());

        List<WorkDayEquipment> existingEquipment = existingWorkDays.stream()
                .flatMap(wd -> wd.getEquipmentUsed().stream())
                .collect(Collectors.toList());

        WorkDayValidationUtils.ComprehensiveValidationResult validationResult =
                WorkDayValidationUtils.validateCompleteWorkDay(
                        workDay, false, existingWorkDays, existingEquipment);

        if (!validationResult.isValid()) {
            throw new InvalidWorkDayTransitionException("Post-update validation misslyckades",
                    "Uppdateringen resulterade i invalid WorkDay state: " +
                            String.join("; ", validationResult.getErrorMessages()));
        }
    }

    /**
     * Processes post-update activities för audit och integration.
     */
    private void processPostUpdateActivities(WorkDay originalWorkDay, WorkDay updatedWorkDay,
                                             UpdateWorkDayDto updateDto) {
        try {
            // Detailed audit logging för update tracking
            logWorkDayUpdate(originalWorkDay, updatedWorkDay, updateDto);

            // Publish update event för system integration
            publishWorkDayUpdatedEvent(originalWorkDay, updatedWorkDay);

            // Update related statistics och cached data
            updateRelatedStatisticsAfterUpdate(updatedWorkDay);

            // Notify affected systems av changes
            notifySystemsOfWorkDayUpdate(originalWorkDay, updatedWorkDay);

        } catch (Exception e) {
            // Log errors men don't fail entire operation
            logNonCriticalError("Post-update processing", e);
        }
    }

    // =================================================================
    // INTEGRATION OCH EVENT PUBLISHING METHODS
    // =================================================================

    /**
     * Publishes WorkDay creation event för system integration.
     */
    private void publishWorkDayCreatedEvent(WorkDay workDay) {
        // I real system: publish to message queue eller event bus
        logBusinessEvent("WORKDAY_CREATED", workDay.getId(),
                "WorkDay skapad för " + workDay.getDate() + " på uppdrag " + workDay.getTask().getNumber());
    }

    /**
     * Publishes WorkDay update event för system integration.
     */
    private void publishWorkDayUpdatedEvent(WorkDay originalWorkDay, WorkDay updatedWorkDay) {
        // I real system: publish detailed change event
        logBusinessEvent("WORKDAY_UPDATED", updatedWorkDay.getId(),
                "WorkDay uppdaterad för " + updatedWorkDay.getDate());
    }

    /**
     * Publishes WorkDay deletion event för system integration.
     */
    private void publishWorkDayDeletedEvent(WorkDay workDay) {
        // I real system: publish deletion event
        logBusinessEvent("WORKDAY_DELETED", workDay.getId(),
                "WorkDay borttagen för " + workDay.getDate());
    }

    // =================================================================
    // PLACEHOLDER METHODS FÖR REAL SYSTEM INTEGRATION
    // =================================================================

    // Note: Dessa methods representerar integration points som skulle
    // implementeras fully i ett complete production system

    private void logWorkDayCreation(WorkDay workDay) {
        // Audit logging implementation
        System.out.println("[AUDIT] WorkDay created: " + workDay.getId());
    }

    private void logWorkDayUpdate(WorkDay original, WorkDay updated, UpdateWorkDayDto updateDto) {
        // Detailed change logging
        System.out.println("[AUDIT] WorkDay updated: " + updated.getId() + " - " + updateDto.getUpdateReason());
    }

    private void logWorkDayDeletion(WorkDay workDay) {
        // Deletion audit logging
        System.out.println("[AUDIT] WorkDay deleted: " + workDay.getId());
    }

    private void logBusinessEvent(String eventType, Long workDayId, String description) {
        System.out.println("[EVENT] " + eventType + " - WorkDay " + workDayId + ": " + description);
    }

    private void logNonCriticalError(String operation, Exception e) {
        System.err.println("[ERROR] Non-critical error i " + operation + ": " + e.getMessage());
    }

    private void updateTaskStatistics(Task task) {
        // Update task-level statistics
    }

    private boolean hasBeenProcessedByPayroll(WorkDay workDay) {
        // Check integration med payroll systems
        return false; // Simplified för example
    }

    private boolean hasActiveAuditTrail(WorkDay workDay) {
        // Check audit requirements
        return false; // Simplified för example
    }

    private boolean isRequiredForProjectReporting(WorkDay workDay) {
        // Check reporting dependencies
        return false; // Simplified för example
    }

    private void archiveWorkDayForAudit(WorkDay workDay) {
        // Archive för audit trail preservation
    }

    private void notifySystemsOfPendingDeletion(WorkDay workDay) {
        // Notify integrated systems
    }

    private void updateDependentStatisticsForDeletion(WorkDay workDay) {
        // Update statistics innan deletion
    }

    private void validateCascadeDeletionSafety(WorkDay workDay) {
        // Validate cascade deletion won't cause constraint violations
    }

    private void refreshRelatedStatistics(WorkDay workDay) {
        // Refresh cached statistics efter deletion
    }

    private void updateRelatedStatisticsAfterUpdate(WorkDay workDay) {
        // Update statistics efter modification
    }

    private void notifySystemsOfWorkDayUpdate(WorkDay original, WorkDay updated) {
        // Notify systems av changes
    }

    private void handleUpdateExecutionError(WorkDay existingWorkDay, UpdateWorkDayDto updateDto, Exception e) {
        // Sophisticated error handling och logging
        logNonCriticalError("Update execution", e);
    }

    // Strategy-specific validation methods (placeholders för full implementation)
    private void validateReplaceAllEmployeeTimesBusinessRules(WorkDay workDay, UpdateWorkDayDto updateDto) {
        // Implement validation för REPLACE_ALL strategy
    }

    private void validateAddOrUpdateEmployeeTimesBusinessRules(WorkDay workDay, UpdateWorkDayDto updateDto) {
        // Implement validation för ADD_OR_UPDATE strategy
    }

    private void validateExplicitEmployeeTimeOperationsBusinessRules(WorkDay workDay, UpdateWorkDayDto updateDto) {
        // Implement validation för EXPLICIT_OPERATIONS strategy
    }

    private void validateReplaceAllEquipmentBusinessRules(WorkDay workDay, UpdateWorkDayDto updateDto) {
        // Implement validation för equipment REPLACE_ALL strategy
    }

    private void validateAddOrUpdateEquipmentBusinessRules(WorkDay workDay, UpdateWorkDayDto updateDto) {
        // Implement validation för equipment ADD_OR_UPDATE strategy
    }

    private void validateExplicitEquipmentOperationsBusinessRules(WorkDay workDay, UpdateWorkDayDto updateDto) {
        // Implement validation för equipment EXPLICIT_OPERATIONS strategy
    }

    // Report generation methods (placeholders för full business intelligence implementation)
    private MonthlyWorkDayReportDto generateComprehensiveMonthlyReport(List<WorkDay> workDays, int year, int month) {
        throw new UnsupportedOperationException("Full monthly report generation would be implemented here");
    }

    private EmployeeProductivityReportDto analyzeEmployeeProductivityFromWorkDays(Employee employee,
                                                                                  List<WorkDay> workDays,
                                                                                  LocalDate startDate,
                                                                                  LocalDate endDate) {
        throw new UnsupportedOperationException("Full productivity analysis would be implemented here");
    }

    // Placeholder DTOs för return types
    public static class MonthlyWorkDayReportDto {
        // Would contain comprehensive monthly metrics
    }

    public static class EmployeeProductivityReportDto {
        // Would contain detailed productivity analysis
    }
}