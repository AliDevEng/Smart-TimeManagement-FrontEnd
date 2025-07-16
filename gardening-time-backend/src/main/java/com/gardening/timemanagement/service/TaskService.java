package com.gardening.timemanagement.service;

import com.gardening.timemanagement.dto.request.CreateTaskDto;
import com.gardening.timemanagement.dto.request.UpdateTaskDto;
import com.gardening.timemanagement.dto.response.TaskResponseDto;
import com.gardening.timemanagement.entity.Customer;
import com.gardening.timemanagement.entity.Task;
import com.gardening.timemanagement.entity.Task.TaskStatus;
import com.gardening.timemanagement.entity.WorkDay;
import com.gardening.timemanagement.exception.TaskNotFoundException;
import com.gardening.timemanagement.exception.InvalidTaskTransitionException;
import com.gardening.timemanagement.exception.TaskDeletionException;
import com.gardening.timemanagement.exception.DuplicateTaskException;
import com.gardening.timemanagement.mapper.TaskMapper;
import com.gardening.timemanagement.repository.TaskRepository;
import com.gardening.timemanagement.repository.CustomerRepository;
import com.gardening.timemanagement.repository.WorkDayRepository;
import com.gardening.timemanagement.util.TaskStatusUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enterprise-nivå service för Task-entiteten med avancerad affärslogik.
 *
 * Denna service representerar det mest sofistikerade lagret i vårt API och ansvarar för:
 * - Komplex state machine-hantering för uppdragsstatus
 * - Transaktionshantering för multi-step business operations
 * - Integration mellan Task, Customer, och WorkDay entiteter
 * - Avancerad validering av affärsregler och dataintegritet
 * - Prestanda-optimerade queries och batch-operationer
 * - Resilient error handling med rich context information
 *
 * Pedagogiska lärdomar från denna service:
 * - Enterprise transaction management patterns
 * - State machine implementation i business logic
 * - Multi-repository coordination och data consistency
 * - Advanced business rule validation strategies
 * - Performance-aware service design för scaling
 */
@Service
@Transactional(readOnly = true) // Default för alla metoder - optimerar läsoperationer
public class TaskService {

    private final TaskRepository taskRepository;
    private final CustomerRepository customerRepository;
    private final WorkDayRepository workDayRepository;
    private final TaskMapper taskMapper;

    // Affärskonstanter för business rule validation
    private static final int MAX_TASKS_PER_CUSTOMER = 50;
    private static final int MAX_TASK_DESCRIPTION_LENGTH = 2000;
    private static final int DAYS_BEFORE_AUTO_ARCHIVE = 90;

    /**
     * Konstruktor med dependency injection.
     * Alla dependencies är final för immutability och thread safety.
     */
    public TaskService(TaskRepository taskRepository,
                       CustomerRepository customerRepository,
                       WorkDayRepository workDayRepository,
                       TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.customerRepository = customerRepository;
        this.workDayRepository = workDayRepository;
        this.taskMapper = taskMapper;
    }

    // ===================================================================
    // GRUNDLÄGGANDE CRUD-OPERATIONER MED BUSINESS LOGIC
    // ===================================================================

    /**
     * Hämtar alla uppdrag med optimerad prestanda.
     * Inkluderar smart sorting och eager loading av customer data.
     */
    public List<TaskResponseDto> getAllTasks() {
        List<Task> tasks = taskRepository.findActiveTasksWithCustomer();
        return taskMapper.toResponseDtoList(tasks);
    }

    /**
     * Hämtar ett specifikt uppdrag med komplett validering.
     *
     * @param id Uppdragets ID
     * @return TaskResponseDto med komplett information
     * @throws TaskNotFoundException om uppdraget inte finns
     */
    public TaskResponseDto getTaskById(Long id) {
        Task task = findTaskByIdOrThrow(id);
        return taskMapper.toResponseDto(task);
    }

    /**
     * Hämtar uppdrag baserat på uppdragsnummer.
     *
     * @param taskNumber Uppdragsnummer att söka efter
     * @return TaskResponseDto om uppdraget finns
     * @throws TaskNotFoundException om uppdraget inte finns
     */
    public TaskResponseDto getTaskByNumber(String taskNumber) {
        Task task = taskRepository.findByNumber(taskNumber)
                .orElseThrow(() -> new TaskNotFoundException(taskNumber));
        return taskMapper.toResponseDto(task);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TaskResponseDto createTask(CreateTaskDto createDto) {
        // Steg 1: Validera input och affärsregler
        validateCreateTaskInput(createDto);

        // Steg 2: Kontrollera att uppdragsnumret är unikt
        if (taskRepository.existsByNumber(createDto.getNumber())) {
            throw new DuplicateTaskException(
                    "Ett uppdrag med nummer '" + createDto.getNumber() + "' finns redan"
            );
        }

        // Steg 3: Hämta och validera customer
        Customer customer = customerRepository.findById(createDto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Kund med ID " + createDto.getCustomerId() + " finns inte"
                ));

        // Steg 4: Kontrollera customer-specifika business rules
        validateCustomerTaskLimits(customer);

        // Steg 5: Konvertera till entity med business logic
        Task task = taskMapper.toEntity(createDto, customer);

        // Steg 6: Tillämpa skapande-specifika business rules
        applyTaskCreationRules(task);

        // Steg 7: Spara och returnera
        Task savedTask = taskRepository.save(task);

        // Steg 8: Loggning för audit trail
        logTaskActivity("CREATED", savedTask.getId(),
                "Nytt uppdrag skapat: " + savedTask.getNumber() + " för " + customer.getName());

        return taskMapper.toResponseDto(savedTask);
    }

    /**
     * Uppdaterar ett befintligt uppdrag med komplex validering.
     *
     * @param id Uppdragets ID
     * @param updateDto Data för uppdateringen
     * @return Det uppdaterade uppdraget
     * @throws TaskNotFoundException om uppdraget inte finns
     * @throws InvalidTaskTransitionException om statusändringar är ogiltiga
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TaskResponseDto updateTask(Long id, UpdateTaskDto updateDto) {
        // Steg 1: Validera input
        validateUpdateTaskInput(updateDto);

        // Steg 2: Hämta befintligt uppdrag
        Task existingTask = findTaskByIdOrThrow(id);

        // Steg 3: Kontrollera att uppdrag kan uppdateras
        validateTaskUpdatePermissions(existingTask, updateDto);

        // Steg 4: Hantera customer-ändring om angiven
        Customer newCustomer = null;
        if (updateDto.getCustomerId() != null) {
            newCustomer = customerRepository.findById(updateDto.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Kund med ID " + updateDto.getCustomerId() + " finns inte"
                    ));
            validateCustomerChange(existingTask, newCustomer);
        }

        // Steg 5: Hantera statusändringar med state machine validation
        if (updateDto.getStatus() != null) {
            TaskStatus newStatus = updateDto.getStatusAsEnum();
            if (newStatus != null && newStatus != existingTask.getStatus()) {
                validateAndExecuteStatusTransition(existingTask, newStatus);
            }
        }

        // Steg 6: Kontrollera uppdragsnummer-unikhet om det ändras
        if (updateDto.getNumber() != null && !updateDto.getNumber().equals(existingTask.getNumber())) {
            if (taskRepository.existsByNumber(updateDto.getNumber())) {
                throw new DuplicateTaskException(
                        "Ett annat uppdrag med nummer '" + updateDto.getNumber() + "' finns redan"
                );
            }
        }

        // Steg 7: Utför actual update genom mapper
        taskMapper.updateEntityFromDto(updateDto, existingTask, newCustomer);

        // Steg 8: Spara och returnera
        Task updatedTask = taskRepository.save(existingTask);

        // Steg 9: Loggning
        logTaskActivity("UPDATED", updatedTask.getId(),
                "Uppdrag uppdaterat: " + updatedTask.getNumber() + " - " + updateDto.getChangesSummary());

        return taskMapper.toResponseDto(updatedTask);
    }

    /**
     * Tar bort ett uppdrag med omfattande validering av beroenden.
     *
     * @param id Uppdragets ID
     * @throws TaskNotFoundException om uppdraget inte finns
     * @throws TaskDeletionException om uppdraget inte kan tas bort
     */
    @Transactional(isolation = Isolation.SERIALIZABLE) // Högsta isolation för delete
    public void deleteTask(Long id) {
        // Steg 1: Hämta uppdraget
        Task task = findTaskByIdOrThrow(id);

        // Steg 2: Kontrollera alla beroenden som förhindrar borttagning
        validateTaskDeletion(task);

        // Steg 3: Loggning före borttagning
        logTaskActivity("DELETED", task.getId(),
                "Uppdrag borttaget: " + task.getNumber() + " för " + task.getCustomer().getName());

        // Steg 4: Utför borttagning
        taskRepository.deleteById(id);
    }

    // ===================================================================
    // ADVANCED BUSINESS OPERATIONS
    // ===================================================================

    /**
     * Ändrar status på ett uppdrag med full state machine-validering.
     *
     * @param id Uppdragets ID
     * @param newStatus Ny status
     * @return Uppdaterat uppdrag
     * @throws InvalidTaskTransitionException om övergången är ogiltig
     */
    @Transactional
    public TaskResponseDto changeTaskStatus(Long id, TaskStatus newStatus) {
        Task task = findTaskByIdOrThrow(id);

        if (task.getStatus() == newStatus) {
            return taskMapper.toResponseDto(task); // Ingen ändring behövs
        }

        validateAndExecuteStatusTransition(task, newStatus);

        Task updatedTask = taskRepository.save(task);

        logTaskActivity("STATUS_CHANGED", updatedTask.getId(),
                "Status ändrad från " + TaskStatusUtils.getDisplayName(task.getStatus()) +
                        " till " + TaskStatusUtils.getDisplayName(newStatus));

        return taskMapper.toResponseDto(updatedTask);
    }

    /**
     * Startar ett uppdrag (sätter startdatum och status till ACTIVE).
     *
     * @param id Uppdragets ID
     * @return Startat uppdrag
     * @throws InvalidTaskTransitionException om uppdraget inte kan startas
     */
    @Transactional
    public TaskResponseDto startTask(Long id) {
        Task task = findTaskByIdOrThrow(id);

        // Validera att uppdraget kan startas
        if (task.getStatus() != TaskStatus.ACTIVE) {
            if (!TaskStatusUtils.isValidTransition(task.getStatus(), TaskStatus.ACTIVE)) {
                throw InvalidTaskTransitionException.cannotReactivateCompleted(
                        task.getId(), task.getNumber()
                );
            }
        }

        // Sätt startdatum om det inte finns
        if (task.getStartDate() == null) {
            task.setStartDate(LocalDate.now());
        }

        // Sätt status till ACTIVE
        task.setStatus(TaskStatus.ACTIVE);

        Task startedTask = taskRepository.save(task);

        logTaskActivity("STARTED", startedTask.getId(),
                "Uppdrag startat: " + startedTask.getNumber());

        return taskMapper.toResponseDto(startedTask);
    }

    /**
     * Avslutar ett uppdrag (sätter slutdatum och status till COMPLETED).
     *
     * @param id Uppdragets ID
     * @return Avslutat uppdrag
     * @throws InvalidTaskTransitionException om uppdraget inte kan avslutas
     */
    @Transactional
    public TaskResponseDto completeTask(Long id) {
        Task task = findTaskByIdOrThrow(id);

        // Validera att uppdraget kan avslutas
        if (!TaskStatusUtils.isValidTransition(task.getStatus(), TaskStatus.COMPLETED)) {
            throw new InvalidTaskTransitionException(
                    task.getId(), task.getNumber(), task.getStatus(), TaskStatus.COMPLETED
            );
        }

        // Kontrollera att uppdraget har startats
        if (task.getStartDate() == null) {
            throw InvalidTaskTransitionException.cannotCompleteUnstarted(
                    task.getId(), task.getNumber()
            );
        }

        // Sätt slutdatum och status
        task.setEndDate(LocalDate.now());
        task.setStatus(TaskStatus.COMPLETED);

        Task completedTask = taskRepository.save(task);

        logTaskActivity("COMPLETED", completedTask.getId(),
                "Uppdrag avslutat: " + completedTask.getNumber());

        return taskMapper.toResponseDto(completedTask);
    }

    /**
     * Avbryter ett uppdrag (sätter slutdatum och status till CANCELLED).
     *
     * @param id Uppdragets ID
     * @param reason Anledning till avbrytande (optional)
     * @return Avbrutet uppdrag
     */
    @Transactional
    public TaskResponseDto cancelTask(Long id, String reason) {
        Task task = findTaskByIdOrThrow(id);

        // Validera att uppdraget kan avbrytas
        if (!TaskStatusUtils.isValidTransition(task.getStatus(), TaskStatus.CANCELLED)) {
            throw new InvalidTaskTransitionException(
                    task.getId(), task.getNumber(), task.getStatus(), TaskStatus.CANCELLED
            );
        }

        // Sätt slutdatum och status
        task.setEndDate(LocalDate.now());
        task.setStatus(TaskStatus.CANCELLED);

        Task cancelledTask = taskRepository.save(task);

        String logMessage = "Uppdrag avbrutet: " + cancelledTask.getNumber();
        if (reason != null && !reason.trim().isEmpty()) {
            logMessage += " - Anledning: " + reason;
        }

        logTaskActivity("CANCELLED", cancelledTask.getId(), logMessage);

        return taskMapper.toResponseDto(cancelledTask);
    }

    // ===================================================================
    // QUERY OPERATIONS OCH RAPPORTERING
    // ===================================================================

    /**
     * Hämtar alla uppdrag för en specifik kund.
     *
     * @param customerId Kundens ID
     * @return Lista av uppdrag för kunden
     */
    public List<TaskResponseDto> getTasksByCustomer(Long customerId) {
        // Validera att kunden finns
        if (!customerRepository.existsById(customerId)) {
            throw new IllegalArgumentException("Kund med ID " + customerId + " finns inte");
        }

        List<Task> tasks = taskRepository.findTasksWithCustomerByCustomerId(customerId);
        return taskMapper.toResponseDtoList(tasks);
    }

    /**
     * Hämtar uppdrag baserat på status.
     *
     * @param status Status att filtrera på
     * @return Lista av uppdrag med angiven status
     */
    public List<TaskResponseDto> getTasksByStatus(TaskStatus status) {
        List<Task> tasks = taskRepository.findByStatus(status);
        return taskMapper.toResponseDtoList(tasks);
    }

    /**
     * Söker uppdrag baserat på uppdragsnummer eller kundnamn.
     *
     * @param searchTerm Sökterm
     * @return Lista av matchande uppdrag
     */
    public List<TaskResponseDto> searchTasks(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllTasks();
        }

        List<Task> tasks = taskRepository.searchTasks(searchTerm.trim());
        return taskMapper.toResponseDtoList(tasks);
    }

    /**
     * Hämtar uppdrag som har arbetstid registrerad inom en period.
     *
     * @param startDate Startdatum för period
     * @param endDate Slutdatum för period
     * @return Lista av uppdrag med arbetstid i perioden
     */
    public List<TaskResponseDto> getTasksWithWorkInPeriod(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        List<Task> tasks = taskRepository.findTasksWithWorkInPeriod(startDate, endDate);
        return taskMapper.toResponseDtoList(tasks);
    }

    /**
     * Genererar månadsstatistik för uppdrag.
     *
     * @param month Månad att generera statistik för
     * @return Map med statistik
     */
    public Map<String, Object> generateMonthlyTaskStatistics(YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<Object[]> statusCounts = taskRepository.countTasksByStatus();
        Map<String, Long> statusStats = statusCounts.stream()
                .collect(Collectors.toMap(
                        row -> TaskStatusUtils.getDisplayName((TaskStatus) row[0]),
                        row -> (Long) row[1]
                ));

        List<Task> monthlyTasks = taskRepository.findTasksWithWorkInPeriod(startDate, endDate);

        return Map.of(
                "month", month.toString(),
                "totalTasks", taskRepository.count(),
                "statusBreakdown", statusStats,
                "tasksWithWorkThisMonth", monthlyTasks.size(),
                "activeCustomers", monthlyTasks.stream()
                        .map(task -> task.getCustomer().getId())
                        .distinct()
                        .count()
        );
    }

    // ===================================================================
    // BUSINESS RULE VALIDATION METHODS
    // ===================================================================

    /**
     * Validerar input för create-operationer.
     */
    private void validateCreateTaskInput(CreateTaskDto createDto) {
        if (createDto == null) {
            throw new IllegalArgumentException("CreateTaskDto kan inte vara null");
        }

        if (!createDto.isValidForCreation()) {
            throw new IllegalArgumentException("CreateTaskDto innehåller ogiltiga data");
        }

        // Ytterligare business rule validering
        if (createDto.getDescription() != null &&
                createDto.getDescription().length() > MAX_TASK_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Beskrivning får inte vara längre än " + MAX_TASK_DESCRIPTION_LENGTH + " tecken"
            );
        }
    }

    /**
     * Validerar input för update-operationer.
     */
    private void validateUpdateTaskInput(UpdateTaskDto updateDto) {
        if (updateDto == null) {
            throw new IllegalArgumentException("UpdateTaskDto kan inte vara null");
        }

        if (!updateDto.hasAnyChanges()) {
            throw new IllegalArgumentException("UpdateTaskDto innehåller inga ändringar");
        }

        // Validera datum-kombination
        if (!updateDto.hasValidDateCombination()) {
            throw new IllegalArgumentException(updateDto.getDateValidationError());
        }
    }

    /**
     * Validerar customer-specifika business rules.
     */
    private void validateCustomerTaskLimits(Customer customer) {
        long taskCount = taskRepository.countActiveTasksForCustomer(customer.getId());
        if (taskCount >= MAX_TASKS_PER_CUSTOMER) {
            throw new IllegalArgumentException(
                    "Kunden " + customer.getName() + " har redan " + taskCount +
                            " aktiva uppdrag. Maximum är " + MAX_TASKS_PER_CUSTOMER
            );
        }
    }

    /**
     * Validerar att ett uppdrag kan uppdateras.
     */
    private void validateTaskUpdatePermissions(Task task, UpdateTaskDto updateDto) {
        // Vissa uppdateringar är inte tillåtna för avslutade uppdrag
        if (TaskStatusUtils.isFinalized(task.getStatus())) {
            // Kontrollera vilka fält som försöks uppdateras
            if (updateDto.getNumber() != null || updateDto.getCustomerId() != null) {
                throw new IllegalArgumentException(
                        "Uppdragsnummer och kund kan inte ändras för uppdrag med status " +
                                TaskStatusUtils.getDisplayName(task.getStatus())
                );
            }
        }
    }

    /**
     * Validerar och utför statusövergång.
     */
    private void validateAndExecuteStatusTransition(Task task, TaskStatus newStatus) {
        if (!TaskStatusUtils.isValidTransition(task.getStatus(), newStatus)) {
            throw new InvalidTaskTransitionException(
                    task.getId(), task.getNumber(), task.getStatus(), newStatus
            );
        }

        // Ytterligare validering baserat på specifik övergång
        if (newStatus == TaskStatus.COMPLETED) {
            if (task.getStartDate() == null) {
                throw InvalidTaskTransitionException.cannotCompleteUnstarted(
                        task.getId(), task.getNumber()
                );
            }
        }

        // Utför övergången
        task.setStatus(newStatus);

        // Automatiska datum-uppdateringar
        if (newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.CANCELLED) {
            if (task.getEndDate() == null) {
                task.setEndDate(LocalDate.now());
            }
        }
    }

    /**
     * Validerar customer-ändringar.
     */
    private void validateCustomerChange(Task task, Customer newCustomer) {
        if (TaskStatusUtils.isFinalized(task.getStatus())) {
            throw new IllegalArgumentException(
                    "Kan inte ändra kund för uppdrag med status " +
                            TaskStatusUtils.getDisplayName(task.getStatus())
            );
        }

        validateCustomerTaskLimits(newCustomer);
    }

    /**
     * Validerar att ett uppdrag kan tas bort.
     */
    private void validateTaskDeletion(Task task) {
        // Kontrollera arbetsdagar
        List<WorkDay> workDays = workDayRepository.findByTaskId(task.getId());
        if (!workDays.isEmpty()) {
            List<String> workDayDates = workDays.stream()
                    .map(wd -> wd.getDate().toString())
                    .collect(Collectors.toList());

            throw TaskDeletionException.dueToActiveWorkDays(
                    task.getId(), task.getNumber(),
                    task.getCustomer().getName(), workDays.size(), workDayDates
            );
        }

        // Här skulle vi kontrollera andra beroenden som utrustning, ekonomiska poster, etc.
    }

    /**
     * Tillämpar business rules för nya uppdrag.
     */
    private void applyTaskCreationRules(Task task) {
        // Automatisk status-hantering
        if (task.getStatus() == TaskStatus.ACTIVE && task.getStartDate() == null) {
            task.setStartDate(LocalDate.now());
        }

        // Rensa slutdatum för nya uppdrag
        task.setEndDate(null);
    }

    // ===================================================================
    // UTILITY METHODS
    // ===================================================================

    /**
     * Hittar Task baserat på ID eller kastar exception.
     */
    private Task findTaskByIdOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    /**
     * Validerar datumintervall.
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start- och slutdatum måste anges");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Startdatum kan inte vara efter slutdatum");
        }
    }

    /**
     * Loggar aktiviteter för audit trail.
     */
    private void logTaskActivity(String action, Long taskId, String details) {
        // I produktion: skicka till audit log-system
        System.out.println(String.format("[TASK_AUDIT] %s - Task ID: %d - %s",
                action, taskId, details));
    }
}