package com.gardening.timemanagement.service;

import com.gardening.timemanagement.dto.response.EmployeeMonthlyReportDto;
import com.gardening.timemanagement.entity.Employee;
import com.gardening.timemanagement.entity.EmployeeTime;
import com.gardening.timemanagement.entity.WorkDay;
import com.gardening.timemanagement.repository.EmployeeRepository;
import com.gardening.timemanagement.repository.WorkDayRepository;
import com.gardening.timemanagement.exception.EmployeeNotFoundException;
import com.gardening.timemanagement.exception.EmployeeDeletionException;
import com.gardening.timemanagement.exception.DuplicateEmployeeException;
import com.gardening.timemanagement.exception.InvalidWorkTimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Service-klass för avancerad medarbetarhantering och arbetstidslogik.
 * Hanterar komplexa affärsregler kring medarbetarstatus, arbetstidsvalidering,
 * och statistikberäkningar för löne- och rapporteringssystem.
 *
 * Använder separata DTO-klasser för clean separation of concerns.
 */
@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final WorkDayRepository workDayRepository;

    // Affärskonstanter för arbetstidsvalidering
    private static final double MIN_REASONABLE_DAILY_HOURS = 0.5;
    private static final double MAX_REASONABLE_DAILY_HOURS = 16.0;
    private static final double STANDARD_WORK_DAY_HOURS = 8.0;
    private static final double OVERTIME_THRESHOLD_WEEKLY = 40.0;

    public EmployeeService(EmployeeRepository employeeRepository,
                           WorkDayRepository workDayRepository) {
        this.employeeRepository = employeeRepository;
        this.workDayRepository = workDayRepository;
    }

    // ===================================================================
    // GRUNDLÄGGANDE MEDARBETARHANTERING
    // ===================================================================

    /**
     * Hämtar alla medarbetare sorterade efter status (aktiva först) och namn.
     */
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .sorted((e1, e2) -> {
                    // Aktiva medarbetare först
                    int statusCompare = Boolean.compare(e2.getIsActive(), e1.getIsActive());
                    if (statusCompare != 0) return statusCompare;
                    // Sedan alfabetiskt efter namn
                    return e1.getName().compareToIgnoreCase(e2.getName());
                })
                .toList();
    }

    /**
     * Hämtar endast aktiva medarbetare som kan tilldelas arbete.
     */
    public List<Employee> getActiveEmployees() {
        return employeeRepository.findByIsActiveTrue()
                .stream()
                .sorted((e1, e2) -> e1.getName().compareToIgnoreCase(e2.getName()))
                .toList();
    }

    /**
     * Hämtar en medarbetare med omfattande validering.
     */
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Medarbetare med ID " + id + " finns inte"));
    }

    /**
     * Skapar en ny medarbetare med affärsregelvalidering.
     */
    @Transactional
    public Employee createEmployee(Employee employee) {
        validateNewEmployee(employee);

        // Kontrollera dubbletter baserat på namn
        if (employeeRepository.existsByName(employee.getName())) {
            throw new DuplicateEmployeeException(
                    "En medarbetare med namnet '" + employee.getName() + "' finns redan"
            );
        }

        // Kontrollera telefonnummer om det finns
        if (employee.getPhone() != null && !employee.getPhone().trim().isEmpty()) {
            if (employeeRepository.existsByPhone(employee.getPhone())) {
                throw new DuplicateEmployeeException(
                        "En medarbetare med telefonnummer '" + employee.getPhone() + "' finns redan"
                );
            }
        }

        // Säkerställ att nya medarbetare är aktiva
        employee.setIsActive(true);

        Employee savedEmployee = employeeRepository.save(employee);

        logEmployeeActivity("CREATED", savedEmployee.getId(),
                "Ny medarbetare skapad: " + savedEmployee.getName());

        return savedEmployee;
    }

    /**
     * Uppdaterar medarbetarinformation med validering.
     */
    @Transactional
    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        Employee existingEmployee = getEmployeeById(id);

        // Kontrollera namn-konflikter
        if (!existingEmployee.getName().equals(updatedEmployee.getName())) {
            if (employeeRepository.existsByName(updatedEmployee.getName())) {
                throw new DuplicateEmployeeException(
                        "En annan medarbetare med namnet '" + updatedEmployee.getName() + "' finns redan"
                );
            }
        }

        // Kontrollera telefon-konflikter
        if (updatedEmployee.getPhone() != null &&
                !updatedEmployee.getPhone().equals(existingEmployee.getPhone())) {
            if (employeeRepository.existsByPhone(updatedEmployee.getPhone())) {
                throw new DuplicateEmployeeException(
                        "En annan medarbetare med telefonnummer '" + updatedEmployee.getPhone() + "' finns redan"
                );
            }
        }

        // Uppdatera tillåtna fält (notera att isActive hanteras separat)
        existingEmployee.setName(updatedEmployee.getName());
        existingEmployee.setPhone(updatedEmployee.getPhone());

        Employee savedEmployee = employeeRepository.save(existingEmployee);

        logEmployeeActivity("UPDATED", savedEmployee.getId(),
                "Medarbetare uppdaterad: " + savedEmployee.getName());

        return savedEmployee;
    }

    // ===================================================================
    // MEDARBETARSTATUS OCH AKTIVERING
    // ===================================================================

    /**
     * Inaktiverar en medarbetare med affärsregelvalidering.
     * Medarbetare med pågående arbete kan inte inaktiveras.
     */
    @Transactional
    public Employee deactivateEmployee(Long id, String reason) {
        Employee employee = getEmployeeById(id);

        if (!employee.getIsActive()) {
            throw new IllegalStateException(
                    "Medarbetaren '" + employee.getName() + "' är redan inaktiv"
            );
        }

        // Kontrollera att medarbetaren inte har pågående arbete idag eller framtida datum
        List<WorkDay> futureWorkDays = workDayRepository.findWorkDaysForEmployee(id)
                .stream()
                .filter(wd -> !wd.getDate().isBefore(LocalDate.now()))
                .toList();

        if (!futureWorkDays.isEmpty()) {
            throw new EmployeeDeletionException(
                    "Medarbetaren '" + employee.getName() + "' kan inte inaktiveras eftersom " +
                            "den har " + futureWorkDays.size() + " registrerade arbetsdagar idag eller i framtiden. " +
                            "Ta bort dessa registreringar först."
            );
        }

        employee.deactivate();
        Employee savedEmployee = employeeRepository.save(employee);

        logEmployeeActivity("DEACTIVATED", savedEmployee.getId(),
                "Medarbetare inaktiverad: " + savedEmployee.getName() +
                        (reason != null ? " - Orsak: " + reason : ""));

        return savedEmployee;
    }

    /**
     * Återaktiverar en inaktiv medarbetare.
     */
    @Transactional
    public Employee reactivateEmployee(Long id, String reason) {
        Employee employee = getEmployeeById(id);

        if (employee.getIsActive()) {
            throw new IllegalStateException(
                    "Medarbetaren '" + employee.getName() + "' är redan aktiv"
            );
        }

        employee.reactivate();
        Employee savedEmployee = employeeRepository.save(employee);

        logEmployeeActivity("REACTIVATED", savedEmployee.getId(),
                "Medarbetare återaktiverad: " + savedEmployee.getName() +
                        (reason != null ? " - Orsak: " + reason : ""));

        return savedEmployee;
    }

    /**
     * Kontrollerar om en medarbetare kan tilldelas nytt arbete.
     */
    public boolean canAssignWork(Long employeeId) {
        Employee employee = getEmployeeById(employeeId);
        return employee.canBeAssignedToWork();
    }

    // ===================================================================
    // ARBETSTIDSBERÄKNINGAR OCH STATISTIK
    // ===================================================================

    /**
     * Beräknar total arbetstid för en medarbetare inom en period.
     * Inkluderar både ordinarie arbetstid och körtid.
     */
    public double getTotalHoursInPeriod(Long employeeId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        Double totalHours = employeeRepository.getTotalHoursForEmployeeInPeriod(
                employeeId, startDate, endDate);

        return totalHours != null ? totalHours : 0.0;
    }

    /**
     * Räknar antal arbetsdagar för en medarbetare inom en period.
     */
    public long getWorkDaysInPeriod(Long employeeId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        return employeeRepository.getWorkDaysCountForEmployeeInPeriod(
                employeeId, startDate, endDate);
    }

    /**
     * Beräknar genomsnittlig arbetstid per dag för en medarbetare.
     */
    public double getAverageHoursPerDay(Long employeeId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        Double avgHours = employeeRepository.getAverageHoursPerDayForEmployee(
                employeeId, startDate, endDate);

        return avgHours != null ? avgHours : 0.0;
    }

    /**
     * Genererar månadsrapport för en medarbetare.
     * Returnerar separat DTO för clean separation.
     */
    public EmployeeMonthlyReportDto generateMonthlyReport(Long employeeId, YearMonth month) {
        Employee employee = getEmployeeById(employeeId);

        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        double totalHours = getTotalHoursInPeriod(employeeId, startDate, endDate);
        long workDays = getWorkDaysInPeriod(employeeId, startDate, endDate);
        double avgHoursPerDay = getAverageHoursPerDay(employeeId, startDate, endDate);

        Double driveHours = employeeRepository.getTotalDriveHoursForEmployeeInPeriod(
                employeeId, startDate, endDate);
        double totalDriveHours = driveHours != null ? driveHours : 0.0;

        // Beräkna övertid (förenklad logik - verkliga system skulle vara mer komplexa)
        double regularHours = Math.min(totalHours, workDays * STANDARD_WORK_DAY_HOURS);
        double overtimeHours = Math.max(0, totalHours - regularHours);

        return new EmployeeMonthlyReportDto(
                employee.getId(),
                employee.getName(),
                employee.getIsActive(),
                month,
                workDays,
                totalHours,
                regularHours,
                overtimeHours,
                totalDriveHours,
                avgHoursPerDay
        );
    }

    /**
     * Identifierar medarbetare med potentiellt problematiska arbetstider.
     */
    public List<Employee> findEmployeesWithUnusualHours(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        return employeeRepository.findEmployeesWithUnusualHours(
                startDate, endDate, MIN_REASONABLE_DAILY_HOURS, MAX_REASONABLE_DAILY_HOURS);
    }

    // ===================================================================
    // ARBETSTIDSVALIDERING
    // ===================================================================

    /**
     * Validerar en arbetstidsregistrering innan sparning.
     * Kontrollerar både tekniska och affärsmässiga regler.
     */
    public void validateWorkTimeEntry(EmployeeTime employeeTime) {
        if (employeeTime == null) {
            throw new InvalidWorkTimeException("Arbetstidsregistrering kan inte vara null");
        }

        // Grundläggande tidsvalidering
        if (!employeeTime.isValidTimeEntry()) {
            throw new InvalidWorkTimeException("Ogiltig tidsregistrering - kontrollera start-/sluttider och lunch");
        }

        // Kontrollera att medarbetaren är aktiv
        Employee employee = employeeTime.getEmployee();
        if (employee != null && !employee.canBeAssignedToWork()) {
            throw new InvalidWorkTimeException(
                    "Medarbetaren '" + employee.getName() + "' är inaktiv och kan inte registrera arbetstid"
            );
        }

        // Kontrollera rimliga arbetstider
        if (!employeeTime.isReasonableWorkDay()) {
            double totalHours = employeeTime.getTotalHours().doubleValue();
            throw new InvalidWorkTimeException(
                    String.format("Arbetstiden %.1f timmar verkar orimlig. Förväntad range: %.1f - %.1f timmar",
                            totalHours, MIN_REASONABLE_DAILY_HOURS, MAX_REASONABLE_DAILY_HOURS)
            );
        }

        // Kontrollera dubbelregistrering
        if (employeeTime.getWorkDay() != null && employeeTime.getEmployee() != null) {
            boolean alreadyRegistered = employeeRepository.isEmployeeRegisteredForWorkDay(
                    employeeTime.getEmployee().getId(),
                    employeeTime.getWorkDay().getDate(),
                    employeeTime.getWorkDay().getTask().getId()
            );

            if (alreadyRegistered) {
                throw new InvalidWorkTimeException(
                        "Medarbetaren '" + employee.getName() + "' är redan registrerad för denna arbetsdag"
                );
            }
        }
    }

    /**
     * Kontrollerar om en medarbetare redan är registrerad för en specifik arbetsdag.
     */
    public boolean isEmployeeRegistered(Long employeeId, LocalDate date, Long taskId) {
        return employeeRepository.isEmployeeRegisteredForWorkDay(employeeId, date, taskId);
    }

    // ===================================================================
    // SÖKFUNKTIONER
    // ===================================================================

    /**
     * Söker medarbetare baserat på namn eller telefonnummer.
     */
    public List<Employee> searchEmployees(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllEmployees();
        }

        return employeeRepository.searchEmployees(searchTerm.trim());
    }

    /**
     * Hämtar medarbetare som arbetade inom en specifik period.
     */
    public List<Employee> getEmployeesWhoWorkedInPeriod(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        return employeeRepository.findEmployeesWhoWorkedInPeriod(startDate, endDate);
    }

    /**
     * Hittar inaktiva medarbetare (som inte arbetat inom en period).
     */
    public List<Employee> getInactiveEmployeesInPeriod(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        return employeeRepository.findInactiveEmployeesInPeriod(startDate, endDate);
    }

    // ===================================================================
    // PRIVATA HJÄLPMETODER
    // ===================================================================

    private void validateNewEmployee(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Medarbetare kan inte vara null");
        }

        if (employee.getName() == null || employee.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Medarbetarens namn måste anges");
        }

        if (employee.getName().trim().length() < 2) {
            throw new IllegalArgumentException("Medarbetarens namn måste vara minst 2 tecken långt");
        }

        if (employee.getName().trim().length() > 255) {
            throw new IllegalArgumentException("Medarbetarens namn får inte vara längre än 255 tecken");
        }

        // Validera telefonnummer om det finns
        if (employee.getPhone() != null && !employee.getPhone().trim().isEmpty()) {
            if (!isValidPhoneNumber(employee.getPhone())) {
                throw new IllegalArgumentException("Ogiltigt telefonnummer format");
            }
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start- och slutdatum måste anges");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Startdatum kan inte vara efter slutdatum");
        }

        // Begränsa hur långt tillbaka i tiden man kan söka (prestanda)
        if (startDate.isBefore(LocalDate.now().minusYears(2))) {
            throw new IllegalArgumentException("Kan inte söka längre tillbaka än 2 år");
        }

        // Begränsa hur stora intervall som tillåts (prestanda)
        if (startDate.until(endDate).getDays() > 366) {
            throw new IllegalArgumentException("Datumintervall kan inte vara längre än 1 år");
        }
    }

    private boolean isValidPhoneNumber(String phone) {
        if (phone == null) return true;

        String cleanPhone = phone.trim().replaceAll("[\\s\\-()]", "");
        return cleanPhone.matches("^(\\+46|0)[1-9]\\d{7,9}$");
    }

    private void logEmployeeActivity(String action, Long employeeId, String details) {
        System.out.println(String.format("[EMPLOYEE_AUDIT] %s - Employee ID: %d - %s",
                action, employeeId, details));
    }
}