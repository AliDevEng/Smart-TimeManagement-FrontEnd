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
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * WorkDayMapper - Transformerar mellan WorkDay entities och DTOs
 *
 * Denna mapper följer en enkel men kraftfull design som fokuserar på:
 * - Tydliga transformation-metoder för varje use case
 * - Konsistent error handling
 * - Läsbar kod som är lätt att underhålla och förstå
 * - Solid Java practices utan överkomplicering
 *
 * Som student är detta en bra example på hur man bygger maintainable
 * mapper-klasser som balanserar funktionalitet med läsbarhet.
 */
@Component
public class WorkDayMapper {

    // Repository dependencies för att hämta relaterade entities
    private final TaskRepository taskRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final EquipmentRepository equipmentRepository;

    // Konstruktor för dependency injection
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
    // HUVUDTRANSFORMATION METODER - Core Functionality
    // =================================================================

    /**
     * Transformerar WorkDay entity till WorkDayResponseDto
     *
     * Detta är huvudmetoden för att skapa API responses. Den inkluderar
     * all nödvändig information för frontend att visa en komplett arbetsdag.
     *
     * @param workDay Entity att transformera
     * @return Response DTO med all data, eller null om input är null
     */
    public WorkDayResponseDto toResponseDto(WorkDay workDay) {
        // Safety check - alltid kontrollera null input
        if (workDay == null) {
            return null;
        }

        // Skapa ny response DTO
        WorkDayResponseDto dto = new WorkDayResponseDto();

        // Populera grundläggande WorkDay-information
        populateCoreWorkDayFields(dto, workDay);

        // Lägg till task och customer information
        populateTaskAndCustomerData(dto, workDay);

        // Lägg till supervisor information om det finns
        populateSupervisorData(dto, workDay);

        // Lägg till employee times information
        populateEmployeeTimesData(dto, workDay);

        // Lägg till equipment information
        populateEquipmentData(dto, workDay);

        // Beräkna och lägg till metrics
        populateMetricsData(dto, workDay);

        // Lägg till status information
        populateStatusData(dto, workDay);

        return dto;
    }

    /**
     * Transformerar lista av WorkDay entities till lista av DTOs
     *
     * Använder stream API för clean, funktionell programmering approach.
     * Detta är en vanlig pattern för bulk transformations.
     *
     * @param workDays Lista av entities
     * @return Lista av response DTOs
     */
    public List<WorkDayResponseDto> toResponseDtoList(List<WorkDay> workDays) {
        if (workDays == null || workDays.isEmpty()) {
            return new ArrayList<>();
        }

        return workDays.stream()
                .map(this::toResponseDto)  // Använd method reference för clean kod
                .collect(Collectors.toList());
    }

    /**
     * Transformerar CreateWorkDayDto till WorkDay entity
     *
     * Denna metod hanterar skapande av nya WorkDay entities från client input.
     * Den löser upp alla ID-referenser till faktiska entities.
     *
     * @param createDto Input från client
     * @return Ny WorkDay entity redo för persistence
     */
    public WorkDay toEntity(CreateWorkDayDto createDto) {
        if (createDto == null) {
            return null;
        }

        // Skapa ny WorkDay entity
        WorkDay workDay = new WorkDay();

        // Sätt grundläggande fält
        workDay.setDate(createDto.getDate());
        workDay.setNotes(createDto.getNotes());

        // Hämta och sätt task (required)
        Task task = findTaskById(createDto.getTaskId());
        workDay.setTask(task);

        // Hämta och sätt supervisor (optional)
        if (createDto.getSupervisorId() != null) {
            Employee supervisor = findEmployeeById(createDto.getSupervisorId());
            workDay.setSupervisor(supervisor);
        }

        // Lägg till employee times
        addEmployeeTimesToWorkDay(workDay, createDto.getEmployeeTimes());

        // Lägg till equipment usage
        addEquipmentToWorkDay(workDay, createDto.getEquipmentUsage());

        return workDay;
    }

    // =================================================================
    // PRIVATE HELPER METHODS - Organiserad funktionalitet
    // =================================================================

    /**
     * Populerar grundläggande WorkDay-fält i response DTO
     */
    private void populateCoreWorkDayFields(WorkDayResponseDto dto, WorkDay workDay) {
        dto.setId(workDay.getId());
        dto.setDate(workDay.getDate());
        dto.setNotes(workDay.getNotes());
        dto.setCreatedAt(workDay.getCreatedAt());
        dto.setUpdatedAt(workDay.getUpdatedAt());

        // Sätt version för optimistic locking (simplified)
        dto.setVersion(workDay.getId()); // Använd ID som version för simplicitet
    }

    /**
     * Populerar task och customer information i response DTO
     */
    private void populateTaskAndCustomerData(WorkDayResponseDto dto, WorkDay workDay) {
        Task task = workDay.getTask();
        if (task == null) {
            return; // Graceful handling av missing task
        }

        // Skapa task summary DTO
        WorkDayResponseDto.TaskSummaryDto taskDto = new WorkDayResponseDto.TaskSummaryDto(
                task.getId(),
                task.getNumber(),
                task.getStatus().name(), // Använd enum name för status
                task.getDescription()
        );
        dto.setTask(taskDto);

        // Lägg till customer information om det finns
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
     * Populerar supervisor information i response DTO
     */
    private void populateSupervisorData(WorkDayResponseDto dto, WorkDay workDay) {
        Employee supervisor = workDay.getSupervisor();
        if (supervisor == null) {
            return; // Ingen supervisor tilldelad
        }

        WorkDayResponseDto.EmployeeSummaryDto supervisorDto = new WorkDayResponseDto.EmployeeSummaryDto(
                supervisor.getId(),
                supervisor.getName(),
                supervisor.getPhone(),
                supervisor.getIsActive()
        );
        dto.setSupervisor(supervisorDto);
    }

    /**
     * Populerar employee times information i response DTO
     */
    private void populateEmployeeTimesData(WorkDayResponseDto dto, WorkDay workDay) {
        List<EmployeeTime> employeeTimes = workDay.getEmployeeTimes();

        if (employeeTimes.isEmpty()) {
            return;
        }

        // Skapa detailed employee time DTOs
        List<WorkDayResponseDto.EmployeeTimeDetailDto> employeeTimeDtos = employeeTimes.stream()
                .map(this::createEmployeeTimeDetailDto)
                .collect(Collectors.toList());
        dto.setEmployeeTimes(employeeTimeDtos);

        // Skapa även summary för snabb overview
        WorkDayResponseDto.EmployeeTimeSummaryDto summaryDto = createEmployeeTimeSummary(employeeTimes);
        dto.setEmployeeTimeSummary(summaryDto);
    }

    /**
     * Skapar en detailed DTO för en employee time
     */
    private WorkDayResponseDto.EmployeeTimeDetailDto createEmployeeTimeDetailDto(EmployeeTime employeeTime) {
        Employee employee = employeeTime.getEmployee();

        // Skapa employee summary först
        WorkDayResponseDto.EmployeeSummaryDto employeeDto = new WorkDayResponseDto.EmployeeSummaryDto(
                employee.getId(),
                employee.getName(),
                employee.getPhone(),
                employee.getIsActive()
        );

        // Beräkna work time description
        String workTimeDescription = String.format("%s-%s (%s timmar)",
                employeeTime.getStartTime().toString(),
                employeeTime.getEndTime().toString(),
                employeeTime.getTotalHours().toString()
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
                employeeTime.getTotalHours(), // Regular hours = total hours för simplicitet
                workTimeDescription
        );
    }

    /**
     * Skapar en summary av alla employee times
     */
    private WorkDayResponseDto.EmployeeTimeSummaryDto createEmployeeTimeSummary(List<EmployeeTime> employeeTimes) {
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

        return new WorkDayResponseDto.EmployeeTimeSummaryDto(
                employeeCount, totalHours, totalDriveHours, averageHours, employeeNames
        );
    }

    /**
     * Populerar equipment information i response DTO
     */
    private void populateEquipmentData(WorkDayResponseDto dto, WorkDay workDay) {
        List<WorkDayEquipment> equipment = workDay.getEquipmentUsed();

        if (equipment.isEmpty()) {
            return;
        }

        // Skapa detailed equipment DTOs
        List<WorkDayResponseDto.EquipmentUsageDetailDto> equipmentDtos = equipment.stream()
                .map(this::createEquipmentUsageDetailDto)
                .collect(Collectors.toList());
        dto.setEquipmentUsage(equipmentDtos);

        // Skapa cost summary
        WorkDayResponseDto.EquipmentCostSummaryDto costSummary = createEquipmentCostSummary(equipment);
        dto.setEquipmentCostSummary(costSummary);
    }

    /**
     * Skapar detailed DTO för equipment usage
     */
    private WorkDayResponseDto.EquipmentUsageDetailDto createEquipmentUsageDetailDto(WorkDayEquipment workDayEquipment) {
        Equipment equipment = workDayEquipment.getEquipment();

        // Skapa equipment summary
        WorkDayResponseDto.EquipmentUsageDetailDto.EquipmentSummaryDto equipmentDto =
                new WorkDayResponseDto.EquipmentUsageDetailDto.EquipmentSummaryDto(
                        equipment.getId(),
                        equipment.getName(),
                        equipment.getDailyPrice(),
                        equipment.getIsActive()
                );

        // Beräkna total cost
        BigDecimal totalCost = equipment.getDailyPrice()
                .multiply(BigDecimal.valueOf(workDayEquipment.getQuantity()));

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

    /**
     * Skapar cost summary för equipment
     */
    private WorkDayResponseDto.EquipmentCostSummaryDto createEquipmentCostSummary(List<WorkDayEquipment> equipment) {
        int itemCount = equipment.size();

        BigDecimal totalCost = equipment.stream()
                .map(wde -> wde.getEquipment().getDailyPrice()
                        .multiply(BigDecimal.valueOf(wde.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> equipmentNames = equipment.stream()
                .map(wde -> wde.getEquipment().getName())
                .collect(Collectors.toList());

        String costBreakdown = equipment.stream()
                .map(wde -> String.format("%s: %s SEK",
                        wde.getEquipment().getName(),
                        wde.getEquipment().getDailyPrice()
                                .multiply(BigDecimal.valueOf(wde.getQuantity()))))
                .collect(Collectors.joining(", "));

        return new WorkDayResponseDto.EquipmentCostSummaryDto(
                itemCount, totalCost, equipmentNames, costBreakdown
        );
    }

    /**
     * Populerar metrics information i response DTO
     */
    private void populateMetricsData(WorkDayResponseDto dto, WorkDay workDay) {
        // Beräkna labor metrics
        BigDecimal totalLaborHours = BigDecimal.valueOf(workDay.getTotalWorkHours());
        BigDecimal hourlyRate = new BigDecimal("500.00"); // Simplified hourly rate
        BigDecimal totalLaborCost = totalLaborHours.multiply(hourlyRate);

        // Beräkna equipment cost
        BigDecimal totalEquipmentCost = workDay.getEquipmentUsed().stream()
                .map(wde -> wde.getEquipment().getDailyPrice()
                        .multiply(BigDecimal.valueOf(wde.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total project cost
        BigDecimal totalProjectCost = totalLaborCost.add(totalEquipmentCost);

        // Simplified productivity calculation
        BigDecimal averageProductivity = totalLaborHours.compareTo(BigDecimal.ZERO) > 0 ?
                totalProjectCost.divide(totalLaborHours, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        WorkDayResponseDto.WorkDayMetricsDto metricsDto = new WorkDayResponseDto.WorkDayMetricsDto(
                totalLaborHours,
                totalLaborCost,
                totalEquipmentCost,
                totalProjectCost,
                averageProductivity,
                "Standard", // Simplified efficiency rating
                "Good"      // Simplified cost effectiveness rating
        );

        dto.setMetrics(metricsDto);
    }

    /**
     * Populerar status information i response DTO
     */
    private void populateStatusData(WorkDayResponseDto dto, WorkDay workDay) {
        // Bestäm overall status baserat på datum
        String overallStatus;
        if (workDay.getDate().isAfter(java.time.LocalDate.now())) {
            overallStatus = "Planerad";
        } else if (workDay.getDate().equals(java.time.LocalDate.now())) {
            overallStatus = "Pågående";
        } else {
            overallStatus = "Slutförd";
        }

        // Bestäm om editable (simplified business rule)
        Boolean isEditable = workDay.getDate().isAfter(java.time.LocalDate.now().minusDays(7));

        // Check för warnings (simplified)
        Boolean hasWarnings = workDay.getTotalWorkHours() > 40.0; // Mer än 40 timmar

        List<String> statusMessages = new ArrayList<>();
        if (hasWarnings) {
            statusMessages.add("Många arbetstimmar registrerade - kontrollera data");
        }
        if (workDay.getSupervisor() == null) {
            statusMessages.add("Ingen arbetsledare tilldelad");
        }

        WorkDayResponseDto.WorkDayStatusDto statusDto = new WorkDayResponseDto.WorkDayStatusDto(
                overallStatus,
                isEditable,
                hasWarnings,
                statusMessages,
                "System", // Simplified last modified by
                workDay.getUpdatedAt()
        );

        dto.setStatus(statusDto);
    }

    // =================================================================
    // ENTITY CREATION HELPER METHODS - För CreateDto transformation
    // =================================================================

    /**
     * Lägger till employee times till WorkDay från CreateDto
     */
    private void addEmployeeTimesToWorkDay(WorkDay workDay, List<CreateWorkDayDto.EmployeeTimeDto> employeeTimeDtos) {
        if (employeeTimeDtos == null || employeeTimeDtos.isEmpty()) {
            throw new IllegalArgumentException("WorkDay måste ha minst en medarbetares arbetstid");
        }

        for (CreateWorkDayDto.EmployeeTimeDto dto : employeeTimeDtos) {
            Employee employee = findEmployeeById(dto.getEmployeeId());

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
     * Lägger till equipment till WorkDay från CreateDto
     */
    private void addEquipmentToWorkDay(WorkDay workDay, List<CreateWorkDayDto.EquipmentUsageDto> equipmentDtos) {
        if (equipmentDtos == null || equipmentDtos.isEmpty()) {
            return; // Equipment är optional
        }

        for (CreateWorkDayDto.EquipmentUsageDto dto : equipmentDtos) {
            Equipment equipment = findEquipmentById(dto.getEquipmentId());

            // Använd WorkDay's addEquipment metod
            workDay.addEquipment(equipment, dto.getQuantity());

            // Sätt notes om det finns
            if (dto.getNotes() != null && !dto.getNotes().isEmpty()) {
                // Hitta den sist tillagda equipment för att sätta notes
                List<WorkDayEquipment> equipmentUsed = workDay.getEquipmentUsed();
                if (!equipmentUsed.isEmpty()) {
                    WorkDayEquipment lastAdded = equipmentUsed.get(equipmentUsed.size() - 1);
                    lastAdded.setNotes(dto.getNotes());
                }
            }
        }
    }

    // =================================================================
    // REPOSITORY LOOKUP METHODS - Safe entity resolution
    // =================================================================

    /**
     * Hittar Task by ID med error handling
     */
    private Task findTaskById(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID måste anges");
        }

        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task med ID " + taskId + " finns inte"));
    }

    /**
     * Hittar Employee by ID med error handling
     */
    private Employee findEmployeeById(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee ID måste anges");
        }

        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee med ID " + employeeId + " finns inte"));
    }

    /**
     * Hittar Equipment by ID med error handling
     */
    private Equipment findEquipmentById(Long equipmentId) {
        if (equipmentId == null) {
            throw new IllegalArgumentException("Equipment ID måste anges");
        }

        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Equipment med ID " + equipmentId + " finns inte"));
    }
}