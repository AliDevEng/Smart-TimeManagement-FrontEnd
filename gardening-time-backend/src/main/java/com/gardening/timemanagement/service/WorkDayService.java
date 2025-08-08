package com.gardening.timemanagement.service;

import com.gardening.timemanagement.dto.request.CreateWorkDayDto;
import com.gardening.timemanagement.dto.request.UpdateWorkDayDto;
import com.gardening.timemanagement.dto.response.WorkDayResponseDto;
import com.gardening.timemanagement.entity.*;
import com.gardening.timemanagement.exception.*;
import com.gardening.timemanagement.mapper.WorkDayMapper;
import com.gardening.timemanagement.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * WorkDayService - Huvudservice för WorkDay business logic
 *
 * Denna service hanterar all business logic för arbetsdagar i systemet.
 * Den följer en enkel men robust design som fokuserar på:
 * - Tydlig CRUD funktionalitet med proper validation
 * - Konsistent error handling
 * - Transaction management för data consistency
 * - Läsbar kod som är lätt att underhålla
 * - Rapportfunktionalitet för frontend integration
 *
 * Som student är detta ett bra exempel på hur man strukturerar
 * service-lager som balanserar funktionalitet med enkelhet.
 */
@Service
@Transactional(readOnly = true) // Default alla metoder till read-only för performance
public class WorkDayService {

    // =================================================================
    // DEPENDENCY INJECTION - Repository och Mapper dependencies
    // =================================================================

    private final WorkDayRepository workDayRepository;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final EquipmentRepository equipmentRepository;
    private final CustomerRepository customerRepository;
    private final WorkDayMapper workDayMapper;

    // Business rule constants
    private static final int MAX_EMPLOYEES_PER_WORKDAY = 20;
    private static final int MAX_EQUIPMENT_ITEMS = 15;
    private static final int MAX_HISTORICAL_DAYS = 90;

    @Autowired
    public WorkDayService(
            WorkDayRepository workDayRepository,
            TaskRepository taskRepository,
            EmployeeRepository employeeRepository,
            EquipmentRepository equipmentRepository,
            CustomerRepository customerRepository,
            WorkDayMapper workDayMapper) {

        this.workDayRepository = workDayRepository;
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
        this.equipmentRepository = equipmentRepository;
        this.customerRepository = customerRepository;
        this.workDayMapper = workDayMapper;
    }

    // =================================================================
    // CORE CRUD OPERATIONS - Grundläggande funktionalitet
    // =================================================================

    /**
     * Skapar en ny WorkDay med full validation
     *
     * Denna metod hanterar skapandet av en ny arbetsdag inklusive all
     * nödvändig validering av business rules och entity references.
     *
     * @param createDto Data för den nya arbetsdagen
     * @return WorkDayResponseDto med den skapade arbetsdagen
     * @throws InvalidWorkDayException när business rules bryts
     * @throws DuplicateWorkDayException när Task+Date redan existerar
     */
    @Transactional
    public WorkDayResponseDto createWorkDay(CreateWorkDayDto createDto) {
        // Steg 1: Validera input
        validateCreateWorkDayInput(createDto);

        // Steg 2: Kontrollera att task existerar och är giltig
        Task task = validateAndGetTask(createDto.getTaskId(), createDto.getDate());

        // Steg 3: Kontrollera för duplikater
        checkForDuplicateWorkDay(task.getId(), createDto.getDate());

        // Steg 4: Skapa WorkDay entity med mapper
        WorkDay workDay = workDayMapper.toEntity(createDto);

        // Steg 5: Spara och returnera response
        WorkDay savedWorkDay = workDayRepository.save(workDay);
        return workDayMapper.toResponseDto(savedWorkDay);
    }

    /**
     * Hämtar en WorkDay by ID
     *
     * @param id WorkDay ID att hämta
     * @return WorkDayResponseDto med data
     * @throws WorkDayNotFoundException när WorkDay inte existerar
     */
    public WorkDayResponseDto getWorkDayById(Long id) {
        WorkDay workDay = findWorkDayById(id);
        return workDayMapper.toResponseDto(workDay);
    }

    /**
     * Hämtar alla WorkDays
     *
     * @return Lista av alla WorkDays
     */
    public List<WorkDayResponseDto> getAllWorkDays() {
        List<WorkDay> workDays = workDayRepository.findAll();
        return workDayMapper.toResponseDtoList(workDays);
    }

    /**
     * Uppdaterar en befintlig WorkDay
     *
     * Denna metod tillämpar partial updates - endast fält som inte är null
     * i updateDto kommer att uppdateras på den befintliga WorkDay.
     *
     * @param id WorkDay ID att uppdatera
     * @param updateDto Fält att uppdatera
     * @return Uppdaterad WorkDayResponseDto
     * @throws WorkDayNotFoundException när WorkDay inte existerar
     * @throws InvalidWorkDayException när update bryter business rules
     */
    @Transactional
    public WorkDayResponseDto updateWorkDay(Long id, UpdateWorkDayDto updateDto) {
        // Hämta befintlig WorkDay
        WorkDay existingWorkDay = findWorkDayById(id);

        // Validera att update är tillåten
        validateWorkDayUpdateAllowed(existingWorkDay);

        // Applicera updates
        applyWorkDayUpdates(existingWorkDay, updateDto);

        // Spara och returnera
        WorkDay savedWorkDay = workDayRepository.save(existingWorkDay);
        return workDayMapper.toResponseDto(savedWorkDay);
    }

    /**
     * Tar bort en WorkDay
     *
     * @param id WorkDay ID att ta bort
     * @throws WorkDayNotFoundException när WorkDay inte existerar
     * @throws WorkDayDeletionException när deletion inte är tillåten
     */
    @Transactional
    public void deleteWorkDay(Long id) {
        WorkDay workDay = findWorkDayById(id);

        // Validera att deletion är tillåten
        validateWorkDayDeletionAllowed(workDay);

        // Ta bort
        workDayRepository.delete(workDay);
    }

    // =================================================================
    // QUERY OPERATIONS - Specifika sökningar
    // =================================================================

    /**
     * Hämtar WorkDays för ett specifikt datum
     *
     * @param date Datum att filtrera på
     * @return Lista av WorkDays för det angivna datumet
     */
    public List<WorkDayResponseDto> getWorkDaysByDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Datum kan inte vara null");
        }

        List<WorkDay> workDays = workDayRepository.findByDate(date);
        return workDayMapper.toResponseDtoList(workDays);
    }

    /**
     * Hämtar WorkDays inom ett datumintervall
     *
     * @param startDate Startdatum för intervallet
     * @param endDate Slutdatum för intervallet
     * @return Lista av WorkDays inom intervallet
     */
    public List<WorkDayResponseDto> getWorkDaysInDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        List<WorkDay> workDays = workDayRepository.findByDateBetween(startDate, endDate);
        return workDayMapper.toResponseDtoList(workDays);
    }

    /**
     * Hämtar WorkDays för ett specifikt Task
     *
     * @param taskId Task ID att filtrera på
     * @return Lista av WorkDays för det angivna Task
     */
    public List<WorkDayResponseDto> getWorkDaysByTask(Long taskId) {
        // Validera att Task existerar
        Task task = findTaskById(taskId);

        List<WorkDay> workDays = workDayRepository.findByTaskId(taskId);
        return workDayMapper.toResponseDtoList(workDays);
    }

    /**
     * Hämtar WorkDays för en specifik Employee
     *
     * @param employeeId Employee ID att filtrera på
     * @return Lista av WorkDays där Employee har arbetat
     */
    public List<WorkDayResponseDto> getWorkDaysByEmployee(Long employeeId) {
        // Validera att Employee existerar
        Employee employee = findEmployeeById(employeeId);

        List<WorkDay> workDays = workDayRepository.findByEmployeeId(employeeId);
        return workDayMapper.toResponseDtoList(workDays);
    }

    /**
     * Hämtar senaste WorkDays för dashboard
     *
     * @param days Antal dagar bakåt att hämta
     * @return Lista av senaste WorkDays
     */
    public List<WorkDayResponseDto> getRecentWorkDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Antal dagar måste vara positivt");
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        return getWorkDaysInDateRange(startDate, endDate);
    }

    // =================================================================
    // BUSINESS OPERATIONS - Specifika affärsoperationer
    // =================================================================

    /**
     * Genererar statistik för WorkDays inom en period
     *
     * @param startDate Startdatum för statistik
     * @param endDate Slutdatum för statistik
     * @return Map med statistikdata
     */
    public Map<String, Object> generateWorkDayStatistics(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        List<WorkDay> workDays = workDayRepository.findByDateBetween(startDate, endDate);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("period", Map.of("start", startDate, "end", endDate));
        statistics.put("totalWorkDays", workDays.size());

        if (!workDays.isEmpty()) {
            // Beräkna grundläggande statistik
            double totalHours = workDays.stream()
                    .mapToDouble(WorkDay::getTotalWorkHours)
                    .sum();
            statistics.put("totalHours", totalHours);

            long uniqueEmployees = workDays.stream()
                    .flatMap(wd -> wd.getEmployeeTimes().stream())
                    .map(et -> et.getEmployee().getId())
                    .distinct()
                    .count();
            statistics.put("uniqueEmployees", uniqueEmployees);

            long uniqueTasks = workDays.stream()
                    .map(wd -> wd.getTask().getId())
                    .distinct()
                    .count();
            statistics.put("uniqueTasks", uniqueTasks);

            statistics.put("averageHoursPerWorkDay", totalHours / workDays.size());
            if (uniqueEmployees > 0) {
                statistics.put("averageHoursPerEmployee", totalHours / uniqueEmployees);
            }
        } else {
            statistics.put("totalHours", 0.0);
            statistics.put("uniqueEmployees", 0);
            statistics.put("uniqueTasks", 0);
            statistics.put("averageHoursPerWorkDay", 0.0);
            statistics.put("averageHoursPerEmployee", 0.0);
        }

        return statistics;
    }

    // =================================================================
    // RAPPORT OPERATIONS - Frontend Integration Support
    // =================================================================

    /**
     * Beräknar arbetstimmar för en specifik medarbetare
     *
     * Denna metod stöder frontend rapportfunktionalitet genom att aggregera
     * arbetstimmar för en medarbetare med optional datumfiltrering.
     * Använder samma error handling patterns som resten av service.
     *
     * @param employeeId Medarbetare att beräkna timmar för
     * @param date Optional specifikt datum (null för alla datum)
     * @return Lista med arbetstidsdata för frontend
     */
    public List<Map<String, Object>> calculateEmployeeWorkHours(Long employeeId, LocalDate date) {
        // Validera att employee existerar med samma pattern som andra metoder
        Employee employee = findEmployeeById(employeeId);

        List<WorkDay> workDays;
        if (date != null) {
            // Filtrera på specifikt datum - använd befintlig repository pattern
            workDays = workDayRepository.findByDate(date).stream()
                    .filter(wd -> wd.getEmployeeTimes().stream()
                            .anyMatch(et -> et.getEmployee().getId().equals(employeeId)))
                    .collect(Collectors.toList());
        } else {
            // Hämta alla arbetsdagar för medarbetaren
            workDays = workDayRepository.findByEmployeeId(employeeId);
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (WorkDay workDay : workDays) {
            // Hitta medarbetarens arbetstid för denna dag
            Optional<EmployeeTime> employeeTime = workDay.getEmployeeTimes().stream()
                    .filter(et -> et.getEmployee().getId().equals(employeeId))
                    .findFirst();

            if (employeeTime.isPresent()) {
                EmployeeTime et = employeeTime.get();

                // Beräkna arbetstimmar med samma logik som frontend förväntar sig
                double workHours = et.getTotalHours().doubleValue();
                double driveTime = et.getDriveTimeHours().doubleValue();

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", workDay.getDate().toString());
                dayData.put("taskNumber", workDay.getTask().getNumber());
                dayData.put("totalHours", workHours);
                dayData.put("driveTime", driveTime);
                dayData.put("startTime", et.getStartTime().toString());
                dayData.put("endTime", et.getEndTime().toString());
                dayData.put("lunchMinutes", et.getLunchMinutes());

                result.add(dayData);
            }
        }

        return result;
    }

    /**
     * Beräknar arbetstimmar för en specifik kunds projekt
     *
     * Denna metod aggregerar all arbetstid som lagts på en kunds uppdrag
     * inom en specifik period. Följer samma validation patterns som andra metoder.
     *
     * @param customerId Kund att beräkna arbetstid för
     * @param date Optional specifikt datum (null för alla datum)
     * @return Lista med arbetstidsdata per uppdrag för kunden
     */
    public List<Map<String, Object>> calculateCustomerWorkHours(Long customerId, LocalDate date) {
        // Validera att customer existerar med samma error handling approach
        Customer customer = findCustomerById(customerId);

        // Hämta alla uppdrag för kunden
        List<Task> customerTasks = taskRepository.findByCustomerId(customerId);

        if (customerTasks.isEmpty()) {
            return new ArrayList<>(); // Inga uppdrag för denna kund
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Task task : customerTasks) {
            List<WorkDay> workDays;
            if (date != null) {
                // Filtrera på specifikt datum och uppdrag
                workDays = workDayRepository.findByDate(date).stream()
                        .filter(wd -> wd.getTask().getId().equals(task.getId()))
                        .collect(Collectors.toList());
            } else {
                // Hämta alla arbetsdagar för uppdraget
                workDays = workDayRepository.findByTaskId(task.getId());
            }

            for (WorkDay workDay : workDays) {
                // Beräkna total arbetstid för dagen med samma logik som frontend
                double totalHours = workDay.getEmployeeTimes().stream()
                        .mapToDouble(et -> et.getTotalHours().doubleValue())
                        .sum();

                int employeeCount = workDay.getEmployeeTimes().size();

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", workDay.getDate().toString());
                dayData.put("taskNumber", task.getNumber());
                dayData.put("totalHours", totalHours);
                dayData.put("employeeCount", employeeCount);
                dayData.put("taskId", task.getId());

                result.add(dayData);
            }
        }

        return result;
    }

    /**
     * Genererar månadsrapport för alla medarbetare
     *
     * Skapar en sammanfattande rapport som visar total arbetstid och körtid
     * för alla medarbetare under en specifik månad. Följer samma validation approach.
     *
     * @param yearMonth Månad att generera rapport för (format: YYYY-MM)
     * @return Månadsrapport med alla medarbetares arbetstid
     */
    public Map<String, Object> generateMonthlyReport(String yearMonth) {
        // Parsa månad från string format med samma error handling pattern
        if (yearMonth == null || !yearMonth.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("Ogiltigt månadsformat. Använd YYYY-MM");
        }

        String[] parts = yearMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        // Validera rimliga värden
        if (year < 2020 || year > 2030 || month < 1 || month > 12) {
            throw new IllegalArgumentException("Ogiltiga datum värden för månad: " + yearMonth);
        }

        // Beräkna startdatum och slutdatum för månaden
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        // Hämta alla medarbetare
        List<Employee> allEmployees = employeeRepository.findAll();

        List<Map<String, Object>> employeeReports = new ArrayList<>();

        for (Employee employee : allEmployees) {
            // Beräkna arbetstimmar för denna medarbetare under månaden
            List<Map<String, Object>> workData = calculateEmployeeWorkHours(employee.getId(), null);

            // Filtrera på månaden med samma logik som frontend
            double totalHours = workData.stream()
                    .filter(wd -> {
                        String dateStr = (String) wd.get("date");
                        return dateStr.startsWith(yearMonth);
                    })
                    .mapToDouble(wd -> (Double) wd.get("totalHours"))
                    .sum();

            double totalDriveHours = workData.stream()
                    .filter(wd -> {
                        String dateStr = (String) wd.get("date");
                        return dateStr.startsWith(yearMonth);
                    })
                    .mapToDouble(wd -> (Double) wd.get("driveTime"))
                    .sum();

            Map<String, Object> employeeReport = new HashMap<>();
            employeeReport.put("employee", employee.getName());
            employeeReport.put("employeeId", employee.getId());
            employeeReport.put("totalHours", totalHours);
            employeeReport.put("totalDriveHours", totalDriveHours);

            employeeReports.add(employeeReport);
        }

        // Skapa sammanfattande rapport med samma structure som frontend förväntar
        Map<String, Object> report = new HashMap<>();
        report.put("month", yearMonth);
        report.put("employeeReports", employeeReports);
        report.put("totalEmployees", allEmployees.size());

        // Beräkna totalsummor
        double grandTotalHours = employeeReports.stream()
                .mapToDouble(er -> (Double) er.get("totalHours"))
                .sum();
        double grandTotalDriveHours = employeeReports.stream()
                .mapToDouble(er -> (Double) er.get("totalDriveHours"))
                .sum();

        report.put("grandTotalHours", grandTotalHours);
        report.put("grandTotalDriveHours", grandTotalDriveHours);

        return report;
    }

    // =================================================================
    // PRIVATE VALIDATION METHODS - Intern validering
    // =================================================================

    /**
     * Validerar input för att skapa WorkDay
     */
    private void validateCreateWorkDayInput(CreateWorkDayDto createDto) {
        if (createDto == null) {
            throw new IllegalArgumentException("CreateWorkDayDto kan inte vara null");
        }

        if (createDto.getDate() == null) {
            // Använd RuntimeException med descriptive message för business rule violations
            throw new RuntimeException("Datum saknas - Arbetsdagens datum måste anges");
        }

        if (createDto.getTaskId() == null) {
            throw new RuntimeException("Task saknas - Task måste anges för arbetsdagen");
        }

        if (createDto.getEmployeeTimes() == null || createDto.getEmployeeTimes().isEmpty()) {
            throw new RuntimeException("Inga arbetstider - Minst en medarbetares arbetstid måste anges");
        }

        // Validera antal medarbetare
        if (createDto.getEmployeeTimes().size() > MAX_EMPLOYEES_PER_WORKDAY) {
            throw new RuntimeException("För många medarbetare - Maximum " + MAX_EMPLOYEES_PER_WORKDAY + " medarbetare per arbetsdag");
        }

        // Validera antal utrustning om det finns
        if (createDto.getEquipmentUsage() != null &&
                createDto.getEquipmentUsage().size() > MAX_EQUIPMENT_ITEMS) {
            throw new RuntimeException("För mycket utrustning - Maximum " + MAX_EQUIPMENT_ITEMS + " utrustningsobjekt per arbetsdag");
        }

        // Validera datum inte är för långt fram i tiden
        if (createDto.getDate().isAfter(LocalDate.now().plusDays(365))) {
            throw new RuntimeException("Datum för långt fram - WorkDay kan inte skapas mer än ett år fram i tiden");
        }
    }

    /**
     * Validerar och hämtar Task
     */
    private Task validateAndGetTask(Long taskId, LocalDate workDayDate) {
        Task task = findTaskById(taskId);

        // Kontrollera att task kan ta emot arbetstid
        if (!task.getStatus().equals(Task.TaskStatus.ACTIVE)) {
            throw new RuntimeException("Task inte aktiv - Endast aktiva uppdrag kan ha arbetstid registrerad");
        }

        return task;
    }

    /**
     * Kontrollerar för duplikat WorkDay
     */
    private void checkForDuplicateWorkDay(Long taskId, LocalDate date) {
        Optional<WorkDay> existingWorkDay = workDayRepository.findByTaskIdAndDate(taskId, date);
        if (existingWorkDay.isPresent()) {
            // Använd enkel String constructor för att undvika constructor signature problem
            throw new DuplicateWorkDayException(
                    "Arbetsdag existerar redan för uppdrag " + taskId + " på datum " + date);
        }
    }

    /**
     * Validerar att WorkDay update är tillåten
     */
    private void validateWorkDayUpdateAllowed(WorkDay workDay) {
        // Kontrollera att WorkDay inte är för gammal
        LocalDate today = LocalDate.now();
        long daysOld = java.time.temporal.ChronoUnit.DAYS.between(workDay.getDate(), today);

        if (daysOld > MAX_HISTORICAL_DAYS) {
            throw new RuntimeException("För gammal för uppdatering - WorkDay är " + daysOld +
                    " dagar gammal. Maximum ålder för uppdatering: " + MAX_HISTORICAL_DAYS + " dagar");
        }
    }

    /**
     * Applicerar updates på WorkDay
     */
    private void applyWorkDayUpdates(WorkDay existingWorkDay, UpdateWorkDayDto updateDto) {
        // Uppdatera endast non-null fält (delta updates)
        if (updateDto.getDate() != null) {
            existingWorkDay.setDate(updateDto.getDate());
        }

        if (updateDto.getNotes() != null) {
            existingWorkDay.setNotes(updateDto.getNotes());
        }

        // Note: I full implementation skulle vi hantera employee times och equipment updates här
        // För nu fokuserar vi på grundläggande fält-updates
    }

    /**
     * Validerar att WorkDay deletion är tillåten
     */
    private void validateWorkDayDeletionAllowed(WorkDay workDay) {
        // Kontrollera ålder
        LocalDate today = LocalDate.now();
        long daysOld = java.time.temporal.ChronoUnit.DAYS.between(workDay.getDate(), today);

        if (daysOld > MAX_HISTORICAL_DAYS) {
            throw new RuntimeException("WorkDay är " + daysOld +
                    " dagar gammal och kan inte tas bort. Maximum ålder för borttagning: " +
                    MAX_HISTORICAL_DAYS + " dagar");
        }

        // Kontrollera att det finns arbetstid registrerad
        if (!workDay.getEmployeeTimes().isEmpty()) {
            throw new RuntimeException("WorkDay har " + workDay.getEmployeeTimes().size() +
                    " arbetstidsregistreringar. Ta bort alla arbetstider innan borttagning av arbetsdagen.");
        }
    }

    /**
     * Validerar datumintervall
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Startdatum kan inte vara null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("Slutdatum kan inte vara null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Startdatum kan inte vara efter slutdatum");
        }

        // Kontrollera att intervallet inte är för stort
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 365) {
            throw new IllegalArgumentException("Datumintervall kan inte vara längre än ett år");
        }
    }

    // =================================================================
    // PRIVATE HELPER METHODS - Gemensamma hjälpmetoder
    // =================================================================

    /**
     * Hittar WorkDay by ID med error handling
     */
    private WorkDay findWorkDayById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("WorkDay ID kan inte vara null");
        }

        return workDayRepository.findById(id)
                .orElseThrow(() -> new WorkDayNotFoundException(
                        "WorkDay med ID " + id + " finns inte"));
    }

    /**
     * Hittar Task by ID med error handling
     */
    private Task findTaskById(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID kan inte vara null");
        }

        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(
                        "Task med ID " + taskId + " finns inte"));
    }

    /**
     * Hittar Employee by ID med error handling
     */
    private Employee findEmployeeById(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee ID kan inte vara null");
        }

        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee med ID " + employeeId + " finns inte"));
    }

    /**
     * Hittar Equipment by ID med error handling
     */
    private Equipment findEquipmentById(Long equipmentId) {
        if (equipmentId == null) {
            throw new IllegalArgumentException("Equipment ID kan inte vara null");
        }

        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Equipment med ID " + equipmentId + " finns inte"));
    }

    /**
     * Hittar Customer by ID med error handling
     *
     * Ny helper metod för rapportfunktionalitet som följer samma pattern
     * som de andra find-metoderna för konsistens.
     */
    private Customer findCustomerById(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID kan inte vara null");
        }

        return customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException(
                        "Customer med ID " + customerId + " finns inte"));
    }
}