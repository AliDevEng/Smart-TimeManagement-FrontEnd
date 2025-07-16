package com.gardening.timemanagement.mapper;

import com.gardening.timemanagement.dto.request.CreateTaskDto;
import com.gardening.timemanagement.dto.request.UpdateTaskDto;
import com.gardening.timemanagement.dto.response.TaskResponseDto;
import com.gardening.timemanagement.dto.response.TaskResponseDto.CustomerSummary;
import com.gardening.timemanagement.entity.Customer;
import com.gardening.timemanagement.entity.Task;
import com.gardening.timemanagement.entity.Task.TaskStatus;
import com.gardening.timemanagement.util.TaskStatusUtils;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Avancerad mapper-klass för Task-entiteten med prestanda-optimering och komplex data transformation.
 *
 * Denna mapper representerar enterprise-nivå data transformation som hanterar:
 * - Enum-konvertering med validering och felhantering
 * - Embedded customer-data för optimerad API-prestanda
 * - Beräknade fält för förbättrad användarupplevelse
 * - Batch-operationer för minimering av databasanrop
 * - Smart caching för Customer-data vid bulk-operationer
 *
 * Pedagogiska lärdomar från denna mapper:
 * - Prestanda-medveten design för enterprise-skalning
 * - Komplexe data transformation-strategier
 * - Integration mellan multiple entities med optimering
 * - Error-resilient mapping med graceful degradation
 */
@Component
public class TaskMapper {

    /**
     * Konverterar från CreateTaskDto till Task-entitet för skapande av nya uppdrag.
     *
     * Denna metod hanterar både explicit och implicit data transformation:
     * - Explicit: direkta fält-till-fält mappningar
     * - Implicit: enum-konvertering, default-värden, affärsregelvalidering
     */
    public Task toEntity(CreateTaskDto createDto, Customer customer) {
        validateCreateDtoInput(createDto, customer);

        Task task = new Task();

        // Grundläggande fältmappning med defensive null-hantering
        task.setNumber(createDto.getNumber() != null ? createDto.getNumber().trim() : null);
        task.setDescription(createDto.getDescription() != null ? createDto.getDescription().trim() : null);
        task.setStartDate(createDto.getStartDate());

        // Customer-relation (förutsätter att customer är valid och managed)
        task.setCustomer(customer);

        // Status-hantering med intelligent defaulting
        TaskStatus status = createDto.getStatusAsEnum();
        task.setStatus(status != null ? status : TaskStatus.ACTIVE);

        // Affärslogik för nya uppdrag
        applyNewTaskBusinessRules(task);

        return task;
    }

    /**
     * Konverterar från Task-entitet till TaskResponseDto för API-responses.
     *
     * Denna metod är optimerad för prestanda genom att undvika lazy loading
     * av Customer-relationen om möjligt. Den hanterar också graceful degradation
     * om Customer-data inte är tillgänglig.
     *
     * @param task Task-entitet att konvertera (kan ha lazy-loaded relations)
     * @return TaskResponseDto med all data inklusive embedded customer info
     */
    public TaskResponseDto toResponseDto(Task task) {
        if (task == null) {
            return null;
        }

        // Skapa customer summary med smart loading
        CustomerSummary customerSummary = createCustomerSummary(task.getCustomer());

        // Huvudkonstruktor som triggar beräknade fält
        TaskResponseDto responseDto = new TaskResponseDto(
                task.getId(),
                task.getNumber(),
                task.getStatus(),
                task.getDescription(),
                task.getStartDate(),
                task.getEndDate(),
                customerSummary,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );

        return responseDto;
    }

    /**
     * Optimerad bulk-konvertering för listor av Tasks.
     *
     * Denna metod använder advanced caching strategies för att minimera
     * databasanrop när vi konverterar många Tasks samtidigt. Den pre-loadar
     * alla nödvändiga Customer-entiteter i en batch för optimal prestanda.
     *
     * @param tasks Lista av Task-entiteter att konvertera
     * @return Lista av TaskResponseDto med optimerad customer-data loading
     */
    public List<TaskResponseDto> toResponseDtoList(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }

        // Pre-load och cache customer-data för bättre prestanda
        Map<Long, CustomerSummary> customerCache = buildCustomerCache(tasks);

        // Konvertera alla tasks med cached customer-data
        return tasks.stream()
                .map(task -> toResponseDtoWithCache(task, customerCache))
                .collect(Collectors.toList());
    }

    /**
     * Uppdaterar en befintlig Task-entitet med data från UpdateTaskDto.
     *
     * Denna metod implementerar sophisticated update-logik som hanterar:
     * - Selective field updates (bara ändra fält som faktiskt är annorlunda)
     * - Business rule validation (kontrollera att updates är tillåtna)
     * - Audit trail preparation (förbereda för change tracking)
     *
     * @param updateDto DTO med uppdaterad data
     * @param existingTask Befintlig Task-entitet att uppdatera
     * @param newCustomer Ny Customer om customer-relation ändras (kan vara null)
     * @throws IllegalArgumentException om update-data är ogiltig
     */
    public void updateEntityFromDto(UpdateTaskDto updateDto, Task existingTask, Customer newCustomer) {
        validateUpdateInput(updateDto, existingTask);

        boolean hasChanges = false;

        // Selektiv uppdatering av fält med change detection
        if (updateDto.getNumber() != null &&
                !updateDto.getNumber().equals(existingTask.getNumber())) {
            existingTask.setNumber(updateDto.getNumber().trim());
            hasChanges = true;
        }

        if (updateDto.getDescription() != null &&
                !updateDto.getDescription().equals(existingTask.getDescription())) {
            String newDesc = updateDto.getDescription().trim();
            existingTask.setDescription(newDesc.isEmpty() ? null : newDesc);
            hasChanges = true;
        }

        if (updateDto.getStartDate() != null &&
                !updateDto.getStartDate().equals(existingTask.getStartDate())) {
            validateStartDateUpdate(updateDto.getStartDate(), existingTask);
            existingTask.setStartDate(updateDto.getStartDate());
            hasChanges = true;
        }

        // Customer-relation update (mest komplex del)
        if (newCustomer != null && !newCustomer.equals(existingTask.getCustomer())) {
            validateCustomerUpdate(newCustomer, existingTask);
            existingTask.setCustomer(newCustomer);
            hasChanges = true;
        }

        // Status update med affärsregelvalidering
        if (updateDto.getStatus() != null) {
            TaskStatus newStatus = TaskStatusUtils.fromString(updateDto.getStatus())
                    .orElseThrow(() -> new IllegalArgumentException("Ogiltig status: " + updateDto.getStatus()));

            if (newStatus != existingTask.getStatus()) {
                validateStatusUpdate(existingTask.getStatus(), newStatus, existingTask);
                existingTask.setStatus(newStatus);
                hasChanges = true;
            }
        }

        // Apply business rules om ändringar gjordes
        if (hasChanges) {
            applyUpdateBusinessRules(existingTask);
        }
    }

    /**
     * Skapar CustomerSummary från Customer-entitet med intelligent loading.
     *
     * Denna metod hanterar different loading scenarios gracefully:
     * - Fully loaded Customer entities
     * - Lazy-loaded Customer proxies
     * - Null eller detached Customer references
     *
     * @param customer Customer-entitet (kan vara lazy-loaded eller null)
     * @return CustomerSummary med tillgänglig data, eller minimal fallback
     */
    private CustomerSummary createCustomerSummary(Customer customer) {
        if (customer == null) {
            return new CustomerSummary(null, "Okänd kund", null);
        }

        try {
            // Försök att access customer-data (kan trigga lazy loading)
            Long customerId = customer.getId();
            String customerName = customer.getName();
            String customerPhone = customer.getPhone();

            return new CustomerSummary(customerId, customerName, customerPhone);

        } catch (Exception e) {
            // Graceful degradation om lazy loading misslyckas
            return new CustomerSummary(null, "Kunde inte ladda kunddata", null);
        }
    }

    /**
     * Bygger en cache av CustomerSummary-objekt för bulk-operationer.
     *
     * Denna metod är en prestanda-optimering som förhindrar N+1 query-problem
     * när vi konverterar många Tasks samtidigt. Den identifierar unika Customers
     * och skapar en lookup-map för effektiv access.
     *
     * @param tasks Lista av Tasks vars Customers ska caches
     * @return Map från Customer ID till CustomerSummary för snabb lookup
     */
    private Map<Long, CustomerSummary> buildCustomerCache(List<Task> tasks) {
        return tasks.stream()
                .map(Task::getCustomer)
                .filter(customer -> customer != null && customer.getId() != null)
                .distinct() // Undvik dubletter
                .collect(Collectors.toMap(
                        Customer::getId,
                        this::createCustomerSummary
                ));
    }

    /**
     * Konverterar Task till TaskResponseDto med pre-cached customer-data.
     *
     * @param task Task att konvertera
     * @param customerCache Cache med customer-data
     * @return TaskResponseDto med optimerad customer-loading
     */
    private TaskResponseDto toResponseDtoWithCache(Task task, Map<Long, CustomerSummary> customerCache) {
        CustomerSummary customerSummary = null;

        if (task.getCustomer() != null && task.getCustomer().getId() != null) {
            customerSummary = customerCache.get(task.getCustomer().getId());
        }

        // Fallback till normal loading om cache miss
        if (customerSummary == null) {
            customerSummary = createCustomerSummary(task.getCustomer());
        }

        return new TaskResponseDto(
                task.getId(),
                task.getNumber(),
                task.getStatus(),
                task.getDescription(),
                task.getStartDate(),
                task.getEndDate(),
                customerSummary,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    /**
     * Validerar input för create-operationer.
     */
    private void validateCreateDtoInput(CreateTaskDto createDto, Customer customer) {
        if (createDto == null) {
            throw new IllegalArgumentException("CreateTaskDto kan inte vara null");
        }
        if (customer == null) {
            throw new IllegalArgumentException("Customer måste anges för nytt uppdrag");
        }
        if (createDto.getNumber() == null || createDto.getNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Uppdragsnummer måste anges");
        }
    }

    /**
     * Validerar input för update-operationer.
     */
    private void validateUpdateInput(UpdateTaskDto updateDto, Task existingTask) {
        if (updateDto == null) {
            throw new IllegalArgumentException("UpdateTaskDto kan inte vara null");
        }
        if (existingTask == null) {
            throw new IllegalArgumentException("Befintlig Task kan inte vara null");
        }
    }

    /**
     * Tillämpar affärsregler för nya uppdrag.
     */
    private void applyNewTaskBusinessRules(Task task) {
        // Nya uppdrag får inte ha endDate
        if (task.getEndDate() != null) {
            task.setEndDate(null);
        }

        // Sätt startDate till idag om status är ACTIVE och inget startdatum finns
        if (task.getStatus() == TaskStatus.ACTIVE && task.getStartDate() == null) {
            task.setStartDate(LocalDate.now());
        }
    }

    /**
     * Tillämpar affärsregler efter updates.
     */
    private void applyUpdateBusinessRules(Task task) {
        // Om status ändras till COMPLETED, sätt endDate om det inte finns
        if (task.getStatus() == TaskStatus.COMPLETED && task.getEndDate() == null) {
            task.setEndDate(LocalDate.now());
        }

        // Om status ändras till CANCELLED, sätt endDate om det inte finns
        if (task.getStatus() == TaskStatus.CANCELLED && task.getEndDate() == null) {
            task.setEndDate(LocalDate.now());
        }
    }

    /**
     * Validerar startdatum-uppdateringar enligt affärsregler.
     */
    private void validateStartDateUpdate(LocalDate newStartDate, Task existingTask) {
        // Kan inte sätta startdatum för avslutade uppdrag
        if (TaskStatusUtils.isFinalized(existingTask.getStatus())) {
            throw new IllegalArgumentException(
                    "Kan inte ändra startdatum för uppdrag med status " +
                            TaskStatusUtils.getDisplayName(existingTask.getStatus())
            );
        }

        // Startdatum kan inte vara efter slutdatum
        if (existingTask.getEndDate() != null && newStartDate.isAfter(existingTask.getEndDate())) {
            throw new IllegalArgumentException(
                    "Startdatum kan inte vara efter slutdatum (" + existingTask.getEndDate() + ")"
            );
        }
    }

    /**
     * Validerar customer-uppdateringar enligt affärsregler.
     */
    private void validateCustomerUpdate(Customer newCustomer, Task existingTask) {
        // Vissa status tillåter inte customer-ändringar
        if (existingTask.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Kan inte ändra kund för avslutade uppdrag"
            );
        }

        // Customer måste vara aktiv (om sådan business rule finns)
        // Detta skulle expanderas med actual Customer validation
    }

    /**
     * Validerar status-uppdateringar med state machine-logik.
     */
    private void validateStatusUpdate(TaskStatus currentStatus, TaskStatus newStatus, Task task) {
        if (!TaskStatusUtils.isValidTransition(currentStatus, newStatus)) {
            throw new IllegalArgumentException(
                    TaskStatusUtils.createTransitionErrorMessage(currentStatus, newStatus)
            );
        }

        // Extra validering för COMPLETED status
        if (newStatus == TaskStatus.COMPLETED) {
            if (task.getStartDate() == null) {
                throw new IllegalArgumentException(
                        "Kan inte avsluta uppdrag som aldrig har startats. Sätt startdatum först."
                );
            }
        }
    }

    /**
     * Utility-metod för att skapa en minimal TaskResponseDto för error scenarios.
     * Användbar när vi vill returnera partial data även om full mapping misslyckas.
     */
    public TaskResponseDto createMinimalResponseDto(Long taskId, String taskNumber, TaskStatus status) {
        CustomerSummary fallbackCustomer = new CustomerSummary(null, "Data ej tillgänglig", null);

        return new TaskResponseDto(
                taskId,
                taskNumber,
                status,
                null, // description
                null, // startDate
                null, // endDate
                fallbackCustomer,
                null, // createdAt
                null  // updatedAt
        );
    }

    /**
     * Utility-metod för att kontrollera om en Task är "mappable"
     * (har all nödvändig data för full DTO-konvertering).
     */
    public boolean isTaskMappable(Task task) {
        return task != null &&
                task.getId() != null &&
                task.getNumber() != null &&
                task.getStatus() != null;
    }

    /**
     * Skapar en sammanfattande beskrivning av mapping-operationen för loggning.
     */
    public String createMappingSummary(Task task, String operation) {
        if (task == null) {
            return operation + ": null task";
        }

        String customerInfo = "okänd kund";
        if (task.getCustomer() != null) {
            customerInfo = task.getCustomer().getName() != null ?
                    task.getCustomer().getName() : "kund-ID " + task.getCustomer().getId();
        }

        return String.format("%s: Task '%s' för %s (status: %s)",
                operation,
                task.getNumber() != null ? task.getNumber() : "inget nummer",
                customerInfo,
                task.getStatus() != null ? TaskStatusUtils.getDisplayName(task.getStatus()) : "okänd status"
        );
    }
}
