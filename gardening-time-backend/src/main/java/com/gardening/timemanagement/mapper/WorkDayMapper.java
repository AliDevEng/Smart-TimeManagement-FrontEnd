package com.gardening.timemanagement.mapper;

import com.gardening.timemanagement.dto.request.CreateWorkDayDto;
import com.gardening.timemanagement.dto.request.UpdateWorkDayDto;
import com.gardening.timemanagement.dto.response.WorkDayResponseDto;
import com.gardening.timemanagement.entity.*;
import com.gardening.timemanagement.repository.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;


@Component
public class WorkDayMapper {

    // Repository dependencies för data access optimization
    private final TaskRepository taskRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final EquipmentRepository equipmentRepository;

    // Expansion level constants för conditional mapping
    public static final String EXPANSION_SUMMARY = "summary";
    public static final String EXPANSION_DETAILED = "detailed";
    public static final String EXPANSION_FULL = "full";
    public static final String EXPANSION_EDIT = "edit";

    // Performance thresholds för intelligent decision making
    private static final int MAX_INLINE_EMPLOYEE_TIMES = 10;
    private static final int MAX_INLINE_EQUIPMENT_ITEMS = 8;
    private static final double EXPENSIVE_CALCULATION_THRESHOLD = 100.0; // Hours

    public WorkDayMapper(TaskRepository taskRepository,
                         CustomerRepository customerRepository,
                         EmployeeRepository employeeRepository,
                         EquipmentRepository equipmentRepository) {
        this.taskRepository = taskRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.equipmentRepository = equipmentRepository;
    }

    // =================================================================
    // PRIMARY TRANSFORMATION METHODS - ENTRY POINTS
    // =================================================================

    /**
     * Transforms WorkDay entity till optimized response DTO med intelligent expansion.
     *
     * Detta är primary transformation method som används för most API responses.
     * Den automatically determines optimal expansion level baserat på data size
     * och performance characteristics.
     *
     * @param workDay Source entity att transformera
     * @return Optimized WorkDayResponseDto
     */
    public WorkDayResponseDto toResponseDto(WorkDay workDay) {
        if (workDay == null) {
            return null;
        }

        // Intelligent expansion level determination baserat på data complexity
        String expansionLevel = determineOptimalExpansionLevel(workDay);

        return toResponseDto(workDay, expansionLevel);
    }

    /**
     * Transforms WorkDay entity med explicit expansion level specification.
     *
     * Denna method ger full control över vilken data som inkluderas i response,
     * vilket är critical för performance optimization i different contexts.
     *
     * @param workDay Source entity
     * @param expansionLevel Desired level of data inclusion
     * @return WorkDayResponseDto optimized för specified expansion level
     */
    public WorkDayResponseDto toResponseDto(WorkDay workDay, String expansionLevel) {
        if (workDay == null) {
            return null;
        }

        // Start med core WorkDay data som alltid inkluderas
        WorkDayResponseDto dto = new WorkDayResponseDto();
        populateCoreFields(dto, workDay);

        // Add related entity data baserat på expansion level
        populateTaskAndCustomerData(dto, workDay, expansionLevel);
        populateSupervisorData(dto, workDay, expansionLevel);

        // Conditional population av expensive collections
        if (shouldIncludeDetailedEmployeeTimes(workDay, expansionLevel)) {
            populateDetailedEmployeeTimes(dto, workDay);
        } else {
            populateEmployeeTimeSummary(dto, workDay);
        }

        if (shouldIncludeDetailedEquipment(workDay, expansionLevel)) {
            populateDetailedEquipment(dto, workDay);
        } else {
            populateEquipmentSummary(dto, workDay);
        }

        // Business intelligence och calculated fields
        if (shouldIncludeMetrics(workDay, expansionLevel)) {
            populateMetrics(dto, workDay);
        }

        if (shouldIncludeStatus(workDay, expansionLevel)) {
            populateStatus(dto, workDay);
        }

        // Response metadata för client optimization
        populateResponseMetadata(dto, workDay, expansionLevel);

        return dto;
    }

    /**
     * Batch transformation för multiple WorkDay entities med performance optimization.
     *
     * Denna method är critical för list endpoints där vi behöver transformera
     * many entities efficiently. Den använder batch loading och caching för
     * att minimera database queries och improve overall performance.
     *
     * @param workDays List av entities att transformera
     * @param expansionLevel Desired expansion level för alla entities
     * @return List av optimized WorkDayResponseDto objects
     */
    public List<WorkDayResponseDto> toResponseDtoList(List<WorkDay> workDays, String expansionLevel) {
        if (workDays == null || workDays.isEmpty()) {
            return new ArrayList<>();
        }

        // Pre-load related entities för batch performance optimization
        preloadRelatedEntitiesForBatch(workDays, expansionLevel);

        // Transform each entity med optimized context
        return workDays.stream()
                .map(workDay -> toResponseDto(workDay, expansionLevel))
                .collect(Collectors.toList());
    }

    // =================================================================
    // CREATE DTO TRANSFORMATION - INPUT PROCESSING
    // =================================================================

    /**
     * Transforms CreateWorkDayDto till WorkDay entity med complete validation.
     *
     * Denna method hanterar complex input processing där nested DTOs måste
     * transformeras till related entities medan vi säkerställer data integrity
     * och business rule compliance.
     *
     * @param createDto Input DTO från client
     * @return New WorkDay entity ready för persistence
     */
    public WorkDay toEntity(CreateWorkDayDto createDto) {
        if (createDto == null) {
            return null;
        }

        // Create main WorkDay entity
        WorkDay workDay = new WorkDay();
        workDay.setDate(createDto.getDate());
        workDay.setNotes(createDto.getNotes());

        // Resolve och set related entities
        Task task = resolveTask(createDto.getTaskId());
        workDay.setTask(task);

        Employee supervisor = resolveSupervisor(createDto.getSupervisorId());
        workDay.setSupervisor(supervisor);

        // Transform nested collections med careful error handling
        transformEmployeeTimesFromCreateDto(workDay, createDto.getEmployeeTimes());
        transformEquipmentUsageFromCreateDto(workDay, createDto.getEquipmentUsage());

        return workDay;
    }

    /**
     * Updates existing WorkDay entity från UpdateWorkDayDto med sophisticated merge logic.
     *
     * Detta är den mest complex transformation method eftersom den måste handle
     * partial updates, collection modifications och optimistic locking medan
     * den preservar data integrity.
     *
     * @param existingWorkDay Current entity state
     * @param updateDto Update instructions från client
     * @return Updated WorkDay entity
     */
    public WorkDay updateEntityFromDto(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        if (existingWorkDay == null || updateDto == null) {
            throw new IllegalArgumentException("Both existing entity and update DTO must be provided");
        }

        // Validate version för optimistic locking
        validateVersionForUpdate(existingWorkDay, updateDto);

        // Update basic fields med null-safe semantics
        updateBasicFields(existingWorkDay, updateDto);

        // Handle related entity updates
        updateTaskIfSpecified(existingWorkDay, updateDto);
        updateSupervisorIfSpecified(existingWorkDay, updateDto);

        // Handle complex collection updates med different strategies
        updateEmployeeTimesAccordingToStrategy(existingWorkDay, updateDto);
        updateEquipmentAccordingToStrategy(existingWorkDay, updateDto);

        return existingWorkDay;
    }

    // =================================================================
    // INTELLIGENT EXPANSION LOGIC - PERFORMANCE OPTIMIZATION
    // =================================================================

    /**
     * Determines optimal expansion level baserat på data characteristics.
     *
     * Denna method implements intelligent decision making för att balance
     * completeness med performance baserat på actual data content.
     */
    private String determineOptimalExpansionLevel(WorkDay workDay) {
        // Factor 1: Collection sizes - larger collections prefer summary
        int employeeCount = workDay.getEmployeeTimes().size();
        int equipmentCount = workDay.getEquipmentUsed().size();

        if (employeeCount > MAX_INLINE_EMPLOYEE_TIMES ||
                equipmentCount > MAX_INLINE_EQUIPMENT_ITEMS) {
            return EXPANSION_SUMMARY;
        }

        // Factor 2: Data complexity - complex calculations prefer caching
        double totalHours = workDay.getTotalWorkHours();
        if (totalHours > EXPENSIVE_CALCULATION_THRESHOLD) {
            return EXPANSION_DETAILED;
        }

        // Factor 3: Data freshness - recent data can handle full expansion
        if (workDay.getUpdatedAt() != null &&
                ChronoUnit.HOURS.between(workDay.getUpdatedAt(), LocalDateTime.now()) < 1) {
            return EXPANSION_FULL;
        }

        // Default för balanced performance
        return EXPANSION_DETAILED;
    }

    /**
     * Determines om detailed employee times ska inkluderas baserat på context.
     */
    private boolean shouldIncludeDetailedEmployeeTimes(WorkDay workDay, String expansionLevel) {
        switch (expansionLevel) {
            case EXPANSION_SUMMARY:
                return false;
            case EXPANSION_DETAILED:
            case EXPANSION_FULL:
            case EXPANSION_EDIT:
                return workDay.getEmployeeTimes().size() <= MAX_INLINE_EMPLOYEE_TIMES;
            default:
                return false;
        }
    }

    /**
     * Determines om detailed equipment data ska inkluderas.
     */
    private boolean shouldIncludeDetailedEquipment(WorkDay workDay, String expansionLevel) {
        switch (expansionLevel) {
            case EXPANSION_SUMMARY:
                return false;
            case EXPANSION_DETAILED:
            case EXPANSION_FULL:
            case EXPANSION_EDIT:
                return workDay.getEquipmentUsed().size() <= MAX_INLINE_EQUIPMENT_ITEMS;
            default:
                return false;
        }
    }

    /**
     * Determines om expensive metrics calculations ska utföras.
     */
    private boolean shouldIncludeMetrics(WorkDay workDay, String expansionLevel) {
        return EXPANSION_DETAILED.equals(expansionLevel) ||
                EXPANSION_FULL.equals(expansionLevel) ||
                EXPANSION_EDIT.equals(expansionLevel);
    }

    /**
     * Determines om status information ska inkluderas.
     */
    private boolean shouldIncludeStatus(WorkDay workDay, String expansionLevel) {
        return !EXPANSION_SUMMARY.equals(expansionLevel);
    }

    // =================================================================
    // CORE FIELD POPULATION - FOUNDATION DATA
    // =================================================================

    /**
     * Populates basic WorkDay fields som alltid inkluderas.
     */
    private void populateCoreFields(WorkDayResponseDto dto, WorkDay workDay) {
        dto.setId(workDay.getId());
        dto.setDate(workDay.getDate());
        dto.setVersion(getCurrentVersion(workDay)); // Mock version för optimistic locking
        dto.setNotes(workDay.getNotes());
        dto.setCreatedAt(workDay.getCreatedAt());
        dto.setUpdatedAt(workDay.getUpdatedAt());
    }

    /**
     * Populates task och customer data med embedded optimization.
     */
    private void populateTaskAndCustomerData(WorkDayResponseDto dto, WorkDay workDay, String expansionLevel) {
        Task task = workDay.getTask();
        if (task == null) {
            return; // Graceful degradation för missing task
        }

        // Create task summary för embedding
        WorkDayResponseDto.TaskSummaryDto taskDto = new WorkDayResponseDto.TaskSummaryDto(
                task.getId(),
                task.getNumber(),
                task.getStatusDisplayName(),
                task.getDescription()
        );
        dto.setTask(taskDto);

        // Embed customer data för performance optimization
        Customer customer = task.getCustomer();
        if (customer != null) {
            WorkDayResponseDto.CustomerSummaryDto customerDto = new WorkDayResponseDto.CustomerSummaryDto(
                    customer.getId(),
                    customer.getName(),
                    customer.getPhone()
            );
            dto.setCustomer(customerDto);
        }
    }

    /**
     * Populates supervisor data om available.
     */
    private void populateSupervisorData(WorkDayResponseDto dto, WorkDay workDay, String expansionLevel) {
        Employee supervisor = workDay.getSupervisor();
        if (supervisor == null) {
            return; // No supervisor assigned
        }

        WorkDayResponseDto.EmployeeSummaryDto supervisorDto = new WorkDayResponseDto.EmployeeSummaryDto(
                supervisor.getId(),
                supervisor.getName(),
                supervisor.getPhone(),
                supervisor.getIsActive()
        );
        dto.setSupervisor(supervisorDto);
    }

    // =================================================================
    // EMPLOYEE TIMES TRANSFORMATION - COMPLEX COLLECTION HANDLING
    // =================================================================

    /**
     * Populates detailed employee time information med full data.
     */
    private void populateDetailedEmployeeTimes(WorkDayResponseDto dto, WorkDay workDay) {
        List<WorkDayResponseDto.EmployeeTimeDetailDto> employeeTimeDtos = workDay.getEmployeeTimes()
                .stream()
                .map(this::transformEmployeeTimeToDetailDto)
                .collect(Collectors.toList());

        dto.setEmployeeTimes(employeeTimeDtos);
    }

    /**
     * Populates condensed employee time summary för performance.
     */
    private void populateEmployeeTimeSummary(WorkDayResponseDto dto, WorkDay workDay) {
        List<EmployeeTime> employeeTimes = workDay.getEmployeeTimes();

        if (employeeTimes.isEmpty()) {
            return;
        }

        // Calculate summary statistics
        int employeeCount = employeeTimes.size();

        BigDecimal totalHours = employeeTimes.stream()
                .map(EmployeeTime::getTotalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDriveHours = employeeTimes.stream()
                .filter(et -> Boolean.TRUE.equals(et.getIsDriver()))
                .map(EmployeeTime::getDriveTimeHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageHours = totalHours.divide(
                BigDecimal.valueOf(employeeCount), 2, RoundingMode.HALF_UP);

        List<String> employeeNames = employeeTimes.stream()
                .map(et -> et.getEmployee().getName())
                .collect(Collectors.toList());

        WorkDayResponseDto.EmployeeTimeSummaryDto summaryDto = new WorkDayResponseDto.EmployeeTimeSummaryDto(
                employeeCount, totalHours, totalDriveHours, averageHours, employeeNames
        );

        dto.setEmployeeTimeSummary(summaryDto);
    }

    /**
     * Transforms individual EmployeeTime till detailed DTO.
     */
    private WorkDayResponseDto.EmployeeTimeDetailDto transformEmployeeTimeToDetailDto(EmployeeTime employeeTime) {
        Employee employee = employeeTime.getEmployee();

        WorkDayResponseDto.EmployeeSummaryDto employeeDto = new WorkDayResponseDto.EmployeeSummaryDto(
                employee.getId(),
                employee.getName(),
                employee.getPhone(),
                employee.getIsActive()
        );

        return new WorkDayResponseDto.EmployeeTimeDetailDto(
                employeeTime.getId(),
                employeeDto,
                employeeTime.getStartTime(),
                employeeTime.getEndTime(),
                employeeTime.getLunchMinutes(),
                employeeTime.getIsDriver(),
                employeeTime.getDriveTimeHours(),
                employeeTime.getTotalHours(),
                employeeTime.getRegularWorkHours(),
                employeeTime.getWorkTimeDescription()
        );
    }

    // =================================================================
    // EQUIPMENT TRANSFORMATION - COST-AWARE MAPPING
    // =================================================================

    /**
     * Populates detailed equipment information med cost calculations.
     */
    private void populateDetailedEquipment(WorkDayResponseDto dto, WorkDay workDay) {
        List<WorkDayResponseDto.EquipmentUsageDetailDto> equipmentDtos = workDay.getEquipmentUsed()
                .stream()
                .map(this::transformEquipmentToDetailDto)
                .collect(Collectors.toList());

        dto.setEquipmentUsage(equipmentDtos);
    }

    /**
     * Populates equipment cost summary för quick cost analysis.
     */
    private void populateEquipmentSummary(WorkDayResponseDto dto, WorkDay workDay) {
        List<WorkDayEquipment> equipment = workDay.getEquipmentUsed();

        if (equipment.isEmpty()) {
            return;
        }

        int itemCount = equipment.size();

        BigDecimal totalCost = equipment.stream()
                .map(WorkDayEquipment::calculateTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> equipmentNames = equipment.stream()
                .map(wde -> wde.getEquipment().getName())
                .collect(Collectors.toList());

        String costBreakdown = generateCostBreakdown(equipment);

        WorkDayResponseDto.EquipmentCostSummaryDto summaryDto = new WorkDayResponseDto.EquipmentCostSummaryDto(
                itemCount, totalCost, equipmentNames, costBreakdown
        );

        dto.setEquipmentCostSummary(summaryDto);
    }

    /**
     * Transforms individual WorkDayEquipment till detailed DTO.
     */
    private WorkDayResponseDto.EquipmentUsageDetailDto transformEquipmentToDetailDto(WorkDayEquipment workDayEquipment) {
        Equipment equipment = workDayEquipment.getEquipment();

        WorkDayResponseDto.EquipmentUsageDetailDto.EquipmentSummaryDto equipmentDto =
                new WorkDayResponseDto.EquipmentUsageDetailDto.EquipmentSummaryDto(
                        equipment.getId(),
                        equipment.getName(),
                        equipment.getDailyPrice(),
                        equipment.getIsActive()
                );

        BigDecimal totalCost = workDayEquipment.calculateTotalCost();
        String costDescription = String.format("%d x %s SEK = %s SEK",
                workDayEquipment.getQuantity(),
                equipment.getDailyPrice().toString(),
                totalCost.toString());

        return new WorkDayResponseDto.EquipmentUsageDetailDto(
                workDayEquipment.getId(),
                equipmentDto,
                workDayEquipment.getQuantity(),
                workDayEquipment.getNotes(),
                equipment.getDailyPrice(),
                totalCost,
                costDescription
        );
    }

    // =================================================================
    // BUSINESS METRICS CALCULATION - INTELLIGENCE LAYER
    // =================================================================

    /**
     * Populates comprehensive business metrics för decision support.
     */
    private void populateMetrics(WorkDayResponseDto dto, WorkDay workDay) {
        // Calculate labor metrics
        BigDecimal totalLaborHours = BigDecimal.valueOf(workDay.getTotalWorkHours());
        BigDecimal totalLaborCost = calculateTotalLaborCost(workDay);

        // Calculate equipment metrics
        BigDecimal totalEquipmentCost = workDay.getEquipmentUsed().stream()
                .map(WorkDayEquipment::calculateTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate project totals
        BigDecimal totalProjectCost = totalLaborCost.add(totalEquipmentCost);

        // Calculate efficiency metrics
        BigDecimal averageProductivity = calculateAverageProductivity(workDay);
        String efficiencyRating = determineEfficiencyRating(workDay);
        String costEffectivenessRating = determineCostEffectivenessRating(totalProjectCost, totalLaborHours);

        WorkDayResponseDto.WorkDayMetricsDto metricsDto = new WorkDayResponseDto.WorkDayMetricsDto(
                totalLaborHours,
                totalLaborCost,
                totalEquipmentCost,
                totalProjectCost,
                averageProductivity,
                efficiencyRating,
                costEffectivenessRating
        );

        dto.setMetrics(metricsDto);
    }

    /**
     * Populates status information för operational awareness.
     */
    private void populateStatus(WorkDayResponseDto dto, WorkDay workDay) {
        String overallStatus = determineOverallStatus(workDay);
        Boolean isEditable = determineIfEditable(workDay);
        Boolean hasWarnings = checkForValidationWarnings(workDay);
        List<String> statusMessages = generateStatusMessages(workDay);

        // Mock user information - i real system skulle detta komma från security context
        String lastModifiedBy = "System"; // Placeholder
        LocalDateTime lastModifiedAt = workDay.getUpdatedAt();

        WorkDayResponseDto.WorkDayStatusDto statusDto = new WorkDayResponseDto.WorkDayStatusDto(
                overallStatus,
                isEditable,
                hasWarnings,
                statusMessages,
                lastModifiedBy,
                lastModifiedAt
        );

        dto.setStatus(statusDto);
    }

    /**
     * Populates response metadata för client optimization guidance.
     */
    private void populateResponseMetadata(WorkDayResponseDto dto, WorkDay workDay, String expansionLevel) {
        List<String> includedFields = determineIncludedFields(dto);
        List<String> availableExpansions = determineAvailableExpansions(workDay);
        Integer cacheRecommendation = calculateCacheRecommendation(workDay, expansionLevel);
        String dataFreshness = calculateDataFreshness(workDay);

        WorkDayResponseDto.ResponseMetadataDto metadataDto = new WorkDayResponseDto.ResponseMetadataDto(
                expansionLevel,
                includedFields,
                availableExpansions,
                cacheRecommendation,
                dataFreshness
        );

        dto.setMetadata(metadataDto);
    }

    // =================================================================
    // HELPER METHODS - BUSINESS LOGIC CALCULATIONS
    // =================================================================

    /**
     * Calculates total labor cost för denna arbetsdag.
     * I real system skulle detta använda configured hourly rates per employee.
     */
    private BigDecimal calculateTotalLaborCost(WorkDay workDay) {
        // Simplified calculation - i production skulle detta vara mer sophisticated
        BigDecimal hourlyRate = new BigDecimal("500.00"); // 500 SEK per hour baseline
        BigDecimal totalHours = BigDecimal.valueOf(workDay.getTotalWorkHours());
        return totalHours.multiply(hourlyRate);
    }

    /**
     * Calculates average productivity metric baserat på work patterns.
     */
    private BigDecimal calculateAverageProductivity(WorkDay workDay) {
        if (workDay.getEmployeeTimes().isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Simplified productivity calculation baserat på hours per employee
        double totalHours = workDay.getTotalWorkHours();
        int employeeCount = workDay.getEmployeeTimes().size();

        double averageHoursPerEmployee = totalHours / employeeCount;

        // Productivity score från 0-100 baserat på standard 8-hour day
        double productivityScore = (averageHoursPerEmployee / 8.0) * 100;

        return BigDecimal.valueOf(Math.min(productivityScore, 100.0))
                .setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * Determines efficiency rating baserat på various factors.
     */
    private String determineEfficiencyRating(WorkDay workDay) {
        double totalHours = workDay.getTotalWorkHours();
        int employeeCount = workDay.getEmployeeTimes().size();

        if (employeeCount == 0) return "N/A";

        double averageHours = totalHours / employeeCount;

        if (averageHours >= 7.5 && averageHours <= 8.5) return "Optimal";
        if (averageHours >= 6.0 && averageHours < 7.5) return "Below Average";
        if (averageHours > 8.5 && averageHours <= 10.0) return "High";
        if (averageHours > 10.0) return "Excessive";

        return "Low";
    }

    /**
     * Determines cost effectiveness rating.
     */
    private String determineCostEffectivenessRating(BigDecimal totalCost, BigDecimal totalHours) {
        if (totalHours.compareTo(BigDecimal.ZERO) == 0) {
            return "N/A";
        }

        BigDecimal costPerHour = totalCost.divide(totalHours, 2, RoundingMode.HALF_UP);

        if (costPerHour.compareTo(new BigDecimal("600")) <= 0) return "Excellent";
        if (costPerHour.compareTo(new BigDecimal("800")) <= 0) return "Good";
        if (costPerHour.compareTo(new BigDecimal("1000")) <= 0) return "Average";

        return "Poor";
    }

    /**
     * Generates cost breakdown description för equipment.
     */
    private String generateCostBreakdown(List<WorkDayEquipment> equipment) {
        if (equipment.isEmpty()) {
            return "Ingen utrustning använd";
        }

        return equipment.stream()
                .map(wde -> String.format("%s: %s SEK",
                        wde.getEquipment().getName(),
                        wde.calculateTotalCost().toString()))
                .collect(Collectors.joining(", "));
    }

    /**
     * Determines overall status för arbetsdagen baserat på multiple factors.
     */
    private String determineOverallStatus(WorkDay workDay) {
        // Check för basic completeness
        if (workDay.getEmployeeTimes().isEmpty()) {
            return "Ofullständig - Ingen arbetstid registrerad";
        }

        // Check för validation issues
        if (checkForValidationWarnings(workDay)) {
            return "Kräver granskning";
        }

        // Check för temporal status
        if (workDay.getDate().isAfter(java.time.LocalDate.now())) {
            return "Planerad";
        }

        if (workDay.getDate().equals(java.time.LocalDate.now())) {
            return "Pågående";
        }

        return "Slutförd";
    }

    /**
     * Determines om arbetsdagen kan editeras baserat på business rules.
     */
    private Boolean determineIfEditable(WorkDay workDay) {
        // Business rule: kan inte editera arbetsdagar äldre än 30 dagar
        java.time.LocalDate cutoffDate = java.time.LocalDate.now().minusDays(30);
        if (workDay.getDate().isBefore(cutoffDate)) {
            return false;
        }

        // Business rule: kan inte editera om task är completed eller cancelled
        if (workDay.getTask() != null && !workDay.getTask().canAcceptWorkTime()) {
            return false;
        }

        return true;
    }

    /**
     * Checks för validation warnings som bör flaggas för user attention.
     */
    private Boolean checkForValidationWarnings(WorkDay workDay) {
        // Check för unusual work patterns
        double totalHours = workDay.getTotalWorkHours();
        if (totalHours > 50.0) { // More than 50 total hours seems unusual
            return true;
        }

        // Check för weekend work
        java.time.DayOfWeek dayOfWeek = workDay.getDate().getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return true;
        }

        // Check för inactive employees
        boolean hasInactiveEmployees = workDay.getEmployeeTimes().stream()
                .anyMatch(et -> !Boolean.TRUE.equals(et.getEmployee().getIsActive()));

        return hasInactiveEmployees;
    }

    /**
     * Generates status messages för user information och guidance.
     */
    private List<String> generateStatusMessages(WorkDay workDay) {
        List<String> messages = new ArrayList<>();

        // Add warnings baserat på business rules
        if (checkForValidationWarnings(workDay)) {
            if (workDay.getTotalWorkHours() > 50.0) {
                messages.add("Varning: Ovanligt många arbetstimmar registrerade");
            }

            java.time.DayOfWeek dayOfWeek = workDay.getDate().getDayOfWeek();
            if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                messages.add("Obs: Arbete utfört på helg - kontrollera övertidsersättning");
            }

            boolean hasInactiveEmployees = workDay.getEmployeeTimes().stream()
                    .anyMatch(et -> !Boolean.TRUE.equals(et.getEmployee().getIsActive()));
            if (hasInactiveEmployees) {
                messages.add("Varning: Innehåller registreringar för inaktiva medarbetare");
            }
        }

        // Add informational messages
        if (workDay.getEquipmentUsed().isEmpty()) {
            messages.add("Information: Ingen utrustning registrerad för denna arbetsdag");
        }

        if (workDay.getSupervisor() == null) {
            messages.add("Information: Ingen arbetsledare tilldelad");
        }

        return messages;
    }

    // =================================================================
    // METADATA CALCULATION METHODS
    // =================================================================

    /**
     * Determines vilka fields som inkluderades i denna response.
     */
    private List<String> determineIncludedFields(WorkDayResponseDto dto) {
        List<String> fields = new ArrayList<>();

        // Always included core fields
        fields.add("id");
        fields.add("date");
        fields.add("version");
        fields.add("task");
        fields.add("customer");

        // Conditional fields
        if (dto.getSupervisor() != null) fields.add("supervisor");
        if (dto.getEmployeeTimes() != null && !dto.getEmployeeTimes().isEmpty()) fields.add("employeeTimes");
        if (dto.getEmployeeTimeSummary() != null) fields.add("employeeTimeSummary");
        if (dto.getEquipmentUsage() != null && !dto.getEquipmentUsage().isEmpty()) fields.add("equipmentUsage");
        if (dto.getEquipmentCostSummary() != null) fields.add("equipmentCostSummary");
        if (dto.getMetrics() != null) fields.add("metrics");
        if (dto.getStatus() != null) fields.add("status");

        return fields;
    }

    /**
     * Determines vilka expansion options som är available för denna WorkDay.
     */
    private List<String> determineAvailableExpansions(WorkDay workDay) {
        List<String> expansions = new ArrayList<>();

        expansions.add(EXPANSION_SUMMARY);
        expansions.add(EXPANSION_DETAILED);

        // FULL expansion bara available för reasonable-sized data
        if (workDay.getEmployeeTimes().size() <= MAX_INLINE_EMPLOYEE_TIMES &&
                workDay.getEquipmentUsed().size() <= MAX_INLINE_EQUIPMENT_ITEMS) {
            expansions.add(EXPANSION_FULL);
        }

        // EDIT expansion available if editable
        if (determineIfEditable(workDay)) {
            expansions.add(EXPANSION_EDIT);
        }

        return expansions;
    }

    /**
     * Calculates optimal cache time för denna response.
     */
    private Integer calculateCacheRecommendation(WorkDay workDay, String expansionLevel) {
        // Base cache time på expansion level
        int baseCacheSeconds = switch (expansionLevel) {
            case EXPANSION_SUMMARY -> 900;      // 15 minutes
            case EXPANSION_DETAILED -> 600;     // 10 minutes
            case EXPANSION_FULL -> 300;         // 5 minutes
            case EXPANSION_EDIT -> 60;          // 1 minute
            default -> 300;
        };

        // Adjust baserat på data age
        if (workDay.getUpdatedAt() != null) {
            long hoursAge = ChronoUnit.HOURS.between(workDay.getUpdatedAt(), LocalDateTime.now());
            if (hoursAge > 24) {
                baseCacheSeconds *= 2; // Older data can be cached longer
            }
        }

        // Adjust baserat på data complexity
        int totalComplexity = workDay.getEmployeeTimes().size() + workDay.getEquipmentUsed().size();
        if (totalComplexity > 10) {
            baseCacheSeconds = Math.max(baseCacheSeconds / 2, 60); // Complex data cached shorter
        }

        return baseCacheSeconds;
    }

    /**
     * Calculates data freshness indicator.
     */
    private String calculateDataFreshness(WorkDay workDay) {
        if (workDay.getUpdatedAt() == null) {
            return "Unknown";
        }

        long minutesAge = ChronoUnit.MINUTES.between(workDay.getUpdatedAt(), LocalDateTime.now());

        if (minutesAge < 5) return "Very Fresh";
        if (minutesAge < 30) return "Fresh";
        if (minutesAge < 120) return "Recent";
        if (minutesAge < 1440) return "Today";

        return "Older";
    }

    // =================================================================
    // ENTITY RESOLUTION METHODS - SAFE LOOKUPS
    // =================================================================

    /**
     * Resolves Task entity med safe error handling.
     */
    private Task resolveTask(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID måste anges för WorkDay");
        }

        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task med ID " + taskId + " finns inte"));
    }

    /**
     * Resolves supervisor Employee med null-safe handling.
     */
    private Employee resolveSupervisor(Long supervisorId) {
        if (supervisorId == null) {
            return null; // No supervisor assigned
        }

        return employeeRepository.findById(supervisorId)
                .orElseThrow(() -> new IllegalArgumentException("Employee med ID " + supervisorId + " finns inte"));
    }

    /**
     * Mock method för att simulera version tracking.
     * I real system skulle detta komma från database entity metadata.
     */
    private Long getCurrentVersion(WorkDay workDay) {
        // Simplified version calculation baserad på update timestamp
        if (workDay.getUpdatedAt() != null) {
            return workDay.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC);
        }
        return 1L;
    }

    // =================================================================
    // UPDATE METHODS - COMPLEX STATE MUTATION HANDLING
    // =================================================================

    /**
     * Validates version för optimistic locking.
     */
    private void validateVersionForUpdate(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        Long currentVersion = getCurrentVersion(existingWorkDay);
        Long expectedVersion = updateDto.getExpectedVersion();

        if (!currentVersion.equals(expectedVersion)) {
            throw new IllegalStateException(
                    String.format("Concurrent modification detected. Expected version %d, but current version is %d",
                            expectedVersion, currentVersion));
        }
    }

    /**
     * Updates basic fields med null-safe semantics.
     */
    private void updateBasicFields(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        // Null means "no change" för alla optional fields
        if (updateDto.getDate() != null) {
            existingWorkDay.setDate(updateDto.getDate());
        }

        if (updateDto.getNotes() != null) {
            if (updateDto.getNotes().isEmpty()) {
                existingWorkDay.setNotes(null); // Empty string means clear notes
            } else {
                existingWorkDay.setNotes(updateDto.getNotes());
            }
        }
    }

    /**
     * Updates task assignment if specified.
     */
    private void updateTaskIfSpecified(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        if (updateDto.getTaskId() != null) {
            Task newTask = resolveTask(updateDto.getTaskId());
            existingWorkDay.setTask(newTask);
        }
    }

    /**
     * Updates supervisor assignment med special null handling.
     */
    private void updateSupervisorIfSpecified(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        if (updateDto.getSupervisorId() != null) {
            if (updateDto.getSupervisorId().equals(-1L)) {
                existingWorkDay.setSupervisor(null); // -1 means remove supervisor
            } else {
                Employee newSupervisor = resolveSupervisor(updateDto.getSupervisorId());
                existingWorkDay.setSupervisor(newSupervisor);
            }
        }
    }

    // =================================================================
    // COLLECTION UPDATE STRATEGIES - COMPLEX MUTATION LOGIC
    // =================================================================

    /**
     * Updates employee times according to specified strategy.
     */
    private void updateEmployeeTimesAccordingToStrategy(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        UpdateWorkDayDto.CollectionUpdateStrategy strategy = updateDto.getEmployeeTimeStrategy();

        switch (strategy) {
            case NO_CHANGE:
                // Do nothing
                break;

            case REPLACE_ALL:
                if (!updateDto.isConfirmDestructiveChanges()) {
                    throw new IllegalArgumentException("REPLACE_ALL strategy kräver confirmDestructiveChanges = true");
                }
                replaceAllEmployeeTimes(existingWorkDay, updateDto.getEmployeeTimeUpdates());
                break;

            case ADD_OR_UPDATE:
                addOrUpdateEmployeeTimes(existingWorkDay, updateDto.getEmployeeTimeUpdates());
                break;

            case EXPLICIT_OPERATIONS:
                performExplicitEmployeeTimeOperations(existingWorkDay, updateDto.getEmployeeTimeUpdates());
                break;
        }
    }

    /**
     * Updates equipment according to specified strategy.
     */
    private void updateEquipmentAccordingToStrategy(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        UpdateWorkDayDto.CollectionUpdateStrategy strategy = updateDto.getEquipmentStrategy();

        switch (strategy) {
            case NO_CHANGE:
                break;

            case REPLACE_ALL:
                if (!updateDto.isConfirmDestructiveChanges()) {
                    throw new IllegalArgumentException("REPLACE_ALL strategy kräver confirmDestructiveChanges = true");
                }
                replaceAllEquipment(existingWorkDay, updateDto.getEquipmentUpdates());
                break;

            case ADD_OR_UPDATE:
                addOrUpdateEquipment(existingWorkDay, updateDto.getEquipmentUpdates());
                break;

            case EXPLICIT_OPERATIONS:
                performExplicitEquipmentOperations(existingWorkDay, updateDto.getEquipmentUpdates());
                break;
        }
    }

    // =================================================================
    // BATCH OPTIMIZATION METHODS - PERFORMANCE CRITICAL
    // =================================================================

    /**
     * Pre-loads related entities för batch transformations för optimal performance.
     */
    private void preloadRelatedEntitiesForBatch(List<WorkDay> workDays, String expansionLevel) {
        if (workDays.isEmpty()) {
            return;
        }

        // Extract all unique IDs för batch loading
        Set<Long> taskIds = workDays.stream()
                .map(wd -> wd.getTask().getId())
                .collect(Collectors.toSet());

        Set<Long> employeeIds = workDays.stream()
                .flatMap(wd -> wd.getEmployeeTimes().stream())
                .map(et -> et.getEmployee().getId())
                .collect(Collectors.toSet());

        Set<Long> equipmentIds = workDays.stream()
                .flatMap(wd -> wd.getEquipmentUsed().stream())
                .map(wde -> wde.getEquipment().getId())
                .collect(Collectors.toSet());

        // Batch load entities to populate Hibernate cache
        // This prevents N+1 query problems during transformation
        if (!taskIds.isEmpty()) {
            taskRepository.findAllById(taskIds);
        }

        if (!employeeIds.isEmpty()) {
            employeeRepository.findAllById(employeeIds);
        }

        if (!equipmentIds.isEmpty()) {
            equipmentRepository.findAllById(equipmentIds);
        }
    }

    // =================================================================
    // CREATE DTO HELPER METHODS - INPUT TRANSFORMATION
    // =================================================================

    /**
     * Transforms employee times från CreateWorkDayDto till EmployeeTime entities.
     */
    private void transformEmployeeTimesFromCreateDto(WorkDay workDay, List<CreateWorkDayDto.EmployeeTimeDto> employeeTimeDtos) {
        if (employeeTimeDtos == null || employeeTimeDtos.isEmpty()) {
            throw new IllegalArgumentException("WorkDay måste ha minst en medarbetares arbetstid");
        }

        for (CreateWorkDayDto.EmployeeTimeDto dto : employeeTimeDtos) {
            Employee employee = employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Employee med ID " + dto.getEmployeeId() + " finns inte"));

            EmployeeTime employeeTime = new EmployeeTime(
                    workDay,
                    employee,
                    dto.getStartTime(),
                    dto.getEndTime(),
                    dto.getLunchMinutes(),
                    dto.getIsDriver(),
                    dto.getDriveTimeHours()
            );

            workDay.addEmployeeTime(employeeTime);
        }
    }

    /**
     * Transforms equipment usage från CreateWorkDayDto till WorkDayEquipment entities.
     */
    private void transformEquipmentUsageFromCreateDto(WorkDay workDay, List<CreateWorkDayDto.EquipmentUsageDto> equipmentDtos) {
        if (equipmentDtos == null) {
            return; // Equipment är optional
        }

        for (CreateWorkDayDto.EquipmentUsageDto dto : equipmentDtos) {
            Equipment equipment = equipmentRepository.findById(dto.getEquipmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Equipment med ID " + dto.getEquipmentId() + " finns inte"));

            WorkDayEquipment workDayEquipment = new WorkDayEquipment(
                    workDay,
                    equipment,
                    dto.getQuantity()
            );
            workDayEquipment.setNotes(dto.getNotes());

            workDay.addEquipment(equipment, dto.getQuantity());
        }
    }

    // =================================================================
    // PLACEHOLDER METHODS FÖR COLLECTION UPDATE OPERATIONS
    // =================================================================

    // Note: Dessa methods skulle implementeras fully i ett complete system
    // Men för pedagogical purposes fokuserar vi på architectural patterns

    private void replaceAllEmployeeTimes(WorkDay workDay, List<UpdateWorkDayDto.EmployeeTimeUpdateDto> updates) {
        // Implementation would clear existing och recreate från updates
        throw new UnsupportedOperationException("Full implementation av REPLACE_ALL strategy");
    }

    private void addOrUpdateEmployeeTimes(WorkDay workDay, List<UpdateWorkDayDto.EmployeeTimeUpdateDto> updates) {
        // Implementation would add new eller update existing based på ID presence
        throw new UnsupportedOperationException("Full implementation av ADD_OR_UPDATE strategy");
    }

    private void performExplicitEmployeeTimeOperations(WorkDay workDay, List<UpdateWorkDayDto.EmployeeTimeUpdateDto> updates) {
        // Implementation would handle ADD, UPDATE, DELETE operations individually
        throw new UnsupportedOperationException("Full implementation av EXPLICIT_OPERATIONS strategy");
    }

    private void replaceAllEquipment(WorkDay workDay, List<UpdateWorkDayDto.EquipmentUsageUpdateDto> updates) {
        throw new UnsupportedOperationException("Full implementation av equipment REPLACE_ALL strategy");
    }

    private void addOrUpdateEquipment(WorkDay workDay, List<UpdateWorkDayDto.EquipmentUsageUpdateDto> updates) {
        throw new UnsupportedOperationException("Full implementation av equipment ADD_OR_UPDATE strategy");
    }

    private void performExplicitEquipmentOperations(WorkDay workDay, List<UpdateWorkDayDto.EquipmentUsageUpdateDto> updates) {
        throw new UnsupportedOperationException("Full implementation av equipment EXPLICIT_OPERATIONS strategy");
    }
}