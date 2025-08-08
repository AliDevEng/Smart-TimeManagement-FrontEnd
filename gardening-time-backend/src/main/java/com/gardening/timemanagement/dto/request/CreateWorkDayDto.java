package com.gardening.timemanagement.dto.request;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;

/**
 * DTO för att skapa en ny arbetsdag.
 *
 * Denna klass representerar all information som behövs för att skapa en komplett
 * arbetsdag inklusive medarbetartider och utrustningsanvändning. Den följer
 * principen om separation of concerns genom att endast fokusera på data som
 * behövs för skapandet av arbetsdagar.
 *
 * Designprinciper:
 * - Enkel och tydlig struktur utan onödiga beroenden
 * - Validering på både fält-nivå och affärslogik-nivå
 * - Stöd för både enkla och komplexa arbetsdagar
 * - Tydlig separation mellan data och affärslogik
 */
public class CreateWorkDayDto {

    // =================================================================
    // GRUNDLÄGGANDE ARBETSDAGSINFORMATION
    // =================================================================

    /**
     * Datum för arbetsdagen.
     * Måste anges och får normalt inte vara i framtiden.
     */
    @NotNull(message = "Arbetsdagens datum måste anges")
    @PastOrPresent(message = "Arbetsdagar kan normalt inte skapas för framtida datum")
    private LocalDate date;

    /**
     * ID för det uppdrag som arbetsdagen utförs för.
     * Måste referera till ett befintligt och aktivt uppdrag.
     */
    @NotNull(message = "Uppdrag måste anges för arbetsdagen")
    @Positive(message = "Uppdrag-ID måste vara ett positivt tal")
    private Long taskId;

    /**
     * ID för arbetsledare (valfritt).
     * Om angivet måste det referera till en aktiv medarbetare.
     */
    @Positive(message = "Arbetsledar-ID måste vara ett positivt tal om angivet")
    private Long supervisorId;

    /**
     * Valfria anteckningar för arbetsdagen.
     * Kan innehålla information om väderförhållanden, speciella omständigheter etc.
     */
    @Size(max = 1000, message = "Anteckningar får inte vara längre än 1000 tecken")
    private String notes;

    /**
     * Lista över medarbetare och deras arbetstider för denna dag.
     * Minst en medarbetare måste anges.
     */
    @NotNull(message = "Medarbetartider måste anges")
    @NotEmpty(message = "Minst en medarbetare måste registrera arbetstid")
    @Valid
    private List<EmployeeTimeDto> employeeTimes;

    /**
     * Lista över utrustning som används under arbetsdagen (valfritt).
     */
    @Valid
    private List<EquipmentUsageDto> equipmentUsage;

    // =================================================================
    // KONSTRUKTORER
    // =================================================================

    /**
     * Standardkonstruktor för JSON-deserialisering.
     */
    public CreateWorkDayDto() {
        this.employeeTimes = new ArrayList<>();
        this.equipmentUsage = new ArrayList<>();
    }

    /**
     * Komplett konstruktor för fullständiga arbetsdagar.
     */
    public CreateWorkDayDto(LocalDate date, Long taskId, Long supervisorId, String notes,
                            List<EmployeeTimeDto> employeeTimes, List<EquipmentUsageDto> equipmentUsage) {
        this.date = date;
        this.taskId = taskId;
        this.supervisorId = supervisorId;
        this.notes = notes;
        this.employeeTimes = employeeTimes != null ? employeeTimes : new ArrayList<>();
        this.equipmentUsage = equipmentUsage != null ? equipmentUsage : new ArrayList<>();
    }

    /**
     * Förenklad konstruktor för vanliga fall utan utrustning.
     */
    public CreateWorkDayDto(LocalDate date, Long taskId, Long supervisorId,
                            List<EmployeeTimeDto> employeeTimes) {
        this(date, taskId, supervisorId, null, employeeTimes, new ArrayList<>());
    }

    /**
     * Minimal konstruktor för grundläggande arbetsdagar.
     */
    public CreateWorkDayDto(LocalDate date, Long taskId, List<EmployeeTimeDto> employeeTimes) {
        this(date, taskId, null, null, employeeTimes, new ArrayList<>());
    }

    // =================================================================
    // BUSINESS LOGIC HJÄLPMETODER
    // =================================================================

    /**
     * Extraherar lista med medarbetar-ID:n för enkel validering.
     * Användbart för att kontrollera att alla medarbetare existerar.
     */
    public List<Long> getEmployeeIds() {
        return employeeTimes.stream()
                .map(EmployeeTimeDto::getEmployeeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * Extraherar lista med utrustnings-ID:n för enkel validering.
     * Användbart för att kontrollera att all utrustning existerar och är tillgänglig.
     */
    public List<Long> getEquipmentIds() {
        if (equipmentUsage == null || equipmentUsage.isEmpty()) {
            return new ArrayList<>();
        }

        return equipmentUsage.stream()
                .map(EquipmentUsageDto::getEquipmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * Kontrollerar om samma medarbetare förekommer flera gånger.
     * Detta är en affärsregel som normalt inte tillåts.
     */
    public boolean hasDuplicateEmployees() {
        if (employeeTimes == null || employeeTimes.size() <= 1) {
            return false;
        }

        Set<Long> employeeIds = new HashSet<>();
        for (EmployeeTimeDto timeDto : employeeTimes) {
            if (timeDto.getEmployeeId() != null) {
                if (!employeeIds.add(timeDto.getEmployeeId())) {
                    return true; // Dublett funnen
                }
            }
        }
        return false;
    }

    /**
     * Kontrollerar om samma utrustning förekommer flera gånger.
     * Detta kan tillåtas i vissa fall men bör valideras.
     */
    public boolean hasDuplicateEquipment() {
        if (equipmentUsage == null || equipmentUsage.size() <= 1) {
            return false;
        }

        Set<Long> equipmentIds = new HashSet<>();
        for (EquipmentUsageDto usage : equipmentUsage) {
            if (usage.getEquipmentId() != null) {
                if (!equipmentIds.add(usage.getEquipmentId())) {
                    return true; // Dublett funnen
                }
            }
        }
        return false;
    }

    /**
     * Räknar antal unika medarbetare.
     * Användbart för snabb validering av affärsregler.
     */
    public int getUniqueEmployeeCount() {
        return getEmployeeIds().size();
    }

    /**
     * Kontrollerar grundläggande affärslogik för arbetsdagen.
     * Denna metod kan användas för snabb validering innan mer komplex server-validering.
     */
    public boolean isValidBasicStructure() {
        // Grundläggande fält måste finnas
        if (date == null || taskId == null || employeeTimes == null || employeeTimes.isEmpty()) {
            return false;
        }

        // Inga dubbletter av medarbetare tillåts
        if (hasDuplicateEmployees()) {
            return false;
        }

        // Alla medarbetartider måste ha giltiga ID:n
        for (EmployeeTimeDto employeeTime : employeeTimes) {
            if (employeeTime.getEmployeeId() == null || employeeTime.getEmployeeId() <= 0) {
                return false;
            }
        }

        return true;
    }

    // =================================================================
    // GETTERS OCH SETTERS
    // =================================================================

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(Long supervisorId) {
        this.supervisorId = supervisorId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<EmployeeTimeDto> getEmployeeTimes() {
        return employeeTimes;
    }

    public void setEmployeeTimes(List<EmployeeTimeDto> employeeTimes) {
        this.employeeTimes = employeeTimes != null ? employeeTimes : new ArrayList<>();
    }

    public List<EquipmentUsageDto> getEquipmentUsage() {
        return equipmentUsage;
    }

    public void setEquipmentUsage(List<EquipmentUsageDto> equipmentUsage) {
        this.equipmentUsage = equipmentUsage != null ? equipmentUsage : new ArrayList<>();
    }

    // =================================================================
    // UTILITY METODER
    // =================================================================

    @Override
    public String toString() {
        return "CreateWorkDayDto{" +
                "date=" + date +
                ", taskId=" + taskId +
                ", supervisorId=" + supervisorId +
                ", notes='" + notes + '\'' +
                ", employeeCount=" + (employeeTimes != null ? employeeTimes.size() : 0) +
                ", equipmentCount=" + (equipmentUsage != null ? equipmentUsage.size() : 0) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateWorkDayDto that = (CreateWorkDayDto) o;
        return Objects.equals(date, that.date) &&
                Objects.equals(taskId, that.taskId) &&
                Objects.equals(supervisorId, that.supervisorId) &&
                Objects.equals(notes, that.notes) &&
                Objects.equals(employeeTimes, that.employeeTimes) &&
                Objects.equals(equipmentUsage, that.equipmentUsage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, taskId, supervisorId, notes, employeeTimes, equipmentUsage);
    }
}

/*
 * VIKTIGA LÄRDOMAR FRÅN DENNA DTO-OMSKRIVNING:
 *
 * 1. SINGLE RESPONSIBILITY: Denna klass har ett enda ansvar - att representera
 *    data för att skapa en arbetsdag. Den blandar inte in update-logik, andra
 *    entiteters validering, eller komplex affärslogik.
 *
 * 2. SEPARATION OF CONCERNS: Validering finns på rätt nivå. @NotNull och liknande
 *    validerar basic constraints, medan affärslogik-validering (som att kontrollera
 *    om uppdrag existerar) hanteras i service-lagret.
 *
 * 3. NESTED DTO COMPOSITION: Vi använder EmployeeTimeDto och EquipmentUsageDto
 *    för att representera komplexa strukturer utan att blanda kod mellan klasserna.
 *
 * 4. DEFENSIVE PROGRAMMING: Konstruktorer och setters ser till att collections
 *    aldrig är null, vilket förhindrar NullPointerExceptions.
 *
 * 5. UTILITY METHODS: Hjälpmetoder som getEmployeeIds() och hasDuplicateEmployees()
 *    gör klassen användbar utan att komplicera dess huvudansvar.
 *
 * 6. PROPER ENCAPSULATION: Alla fält är private med public getters/setters,
 *    vilket följer Java Bean-konventioner och gör klassen kompatibel med
 *    JSON-serialisering och validering frameworks.
 */