package com.gardening.timemanagement.dto.request;

import com.gardening.timemanagement.util.WorkDayValidationUtils;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

/**
 * Avancerad DTO för att skapa en komplett arbetsdag med alla dess komponenter.
 *
 * CreateWorkDayDto representerar den mest komplexa API-strukturen i vårt system
 * eftersom den måste hantera sammansatt input för en hel affärsprocess som
 * involverar Tasks, Employees, Equipment och EmployeeTime i koordinerade
 * nested collections.
 *
 * Denna DTO demonstrerar enterprise-patterns för:
 * - Nested DTO composition med cross-collection validation
 * - Complex business rule integration i API contracts
 * - Performance-conscious design för bulk operations
 * - Rich validation messaging för superior user experience
 *
 * Designfilosofi: "Make the common case easy, the complex case possible"
 * - Enkla arbetsdagar (få medarbetare, ingen utrustning) kräver minimal input
 * - Komplexa projektdagar kan representeras fullständigt med alla detaljer
 */
public class CreateWorkDayDto {

    // =================================================================
    // GRUNDLÄGGANDE ARBETSDAGSINFORMATION
    // =================================================================

    /**
     * Datum för arbetsdagen.
     *
     * Detta fält koordineras med WorkDayValidationUtils för att säkerställa
     * att datum följer affärsregler för temporal validering och historiska
     * begränsningar.
     */
    @NotNull(message = "Arbetsdagens datum måste anges")
    @PastOrPresent(message = "Arbetsdagar kan normalt inte skapas för framtida datum")
    private LocalDate date;

    /**
     * ID för det uppdrag som arbetsdagen utförs för.
     *
     * Refererar till en befintlig Task-entitet som måste vara aktiv och
     * kunna ta emot arbetstidsregistreringar enligt affärsregler.
     */
    @NotNull(message = "Uppdrag måste anges för arbetsdagen")
    @Positive(message = "Uppdrag-ID måste vara ett positivt tal")
    private Long taskId;

    /**
     * ID för arbetsledare (valfritt).
     *
     * Om angivet måste detta referera till en aktiv Employee som kan
     * tilldelas arbete. Arbetsledaren kan också vara en av medarbetarna
     * som registrerar arbetstid.
     */
    @Positive(message = "Arbetsledar-ID måste vara ett positivt tal om angivet")
    private Long supervisorId;

    /**
     * Valfria anteckningar för arbetsdagen.
     *
     * Kan innehålla information om speciella omständigheter, väderförhållanden,
     * problem som uppstått, eller andra detaljer som är relevanta för projektet.
     */
    @Size(max = 2000, message = "Anteckningar får inte vara längre än 2000 tecken")
    private String notes;

    // =================================================================
    // NESTED COLLECTIONS - HJÄRTAT AV KOMPLEXITETEN
    // =================================================================

    /**
     * Lista över medarbetartider för denna arbetsdag.
     *
     * Detta är kärnfunktionaliteten - varje element representerar en
     * medarbetares arbetstid inklusive starttid, sluttid, lunch och körtid.
     *
     * Affärsregler:
     * - Minst en medarbetare måste finnas
     * - Ingen medarbetare får förekomma flera gånger
     * - Alla medarbetare måste vara aktiva och kunna tilldelas arbete
     * - Arbetstider måste vara rimliga och följa företagspolicy
     */
    @NotEmpty(message = "Minst en medarbetare måste tilldelas arbetsdagen")
    @Size(max = 20, message = "Maximum 20 medarbetare per arbetsdag")
    @Valid
    private List<EmployeeTimeDto> employeeTimes = new ArrayList<>();

    /**
     * Lista över utrustning som används under arbetsdagen (valfritt).
     *
     * Varje element representerar en typ av utrustning och kvantitet.
     * Detta är viktigt för kostnadsspårning och resursplanering.
     *
     * Affärsregler:
     * - Alla utrustningsobjekt måste vara aktiva och tillgängliga
     * - Ingen utrustning får listas flera gånger för samma dag
     * - Kvantiteter måste vara rimliga
     */
    @Size(max = 15, message = "Maximum 15 olika utrustningsobjekt per arbetsdag")
    @Valid
    private List<EquipmentUsageDto> equipmentUsage = new ArrayList<>();

    // =================================================================
    // NESTED DTO CLASSES - KOMPOSITIONELLA BYGGSTENAR
    // =================================================================

    /**
     * DTO för en medarbetares arbetstid på denna arbetsdag.
     *
     * Denna nested class kapslar in all information som behövs för att
     * registrera en medarbetares arbetstid, inklusive specialfall som
     * körning och varierande lunchtider.
     */
    public static class EmployeeTimeDto {

        /**
         * ID för medarbetaren som utförde arbetet.
         */
        @NotNull(message = "Medarbetare måste anges för arbetstidsregistrering")
        @Positive(message = "Medarbetar-ID måste vara ett positivt tal")
        private Long employeeId;

        /**
         * Tid när medarbetaren började arbeta.
         */
        @NotNull(message = "Starttid måste anges")
        private LocalTime startTime;

        /**
         * Tid när medarbetaren slutade arbeta.
         */
        @NotNull(message = "Sluttid måste anges")
        private LocalTime endTime;

        /**
         * Lunchtid i minuter (0 om ingen lunch togs).
         */
        @NotNull(message = "Lunchtid måste anges (kan vara 0)")
        @Min(value = 0, message = "Lunchtid kan inte vara negativ")
        @Max(value = 120, message = "Lunchtid kan inte vara längre än 120 minuter")
        private Integer lunchMinutes = 0;

        /**
         * Om medarbetaren fungerade som förare denna dag.
         */
        @NotNull(message = "Förare-status måste anges")
        private Boolean isDriver = false;

        /**
         * Körtid i timmar (endast relevant om isDriver = true).
         *
         * Denna tid läggs till ordinarie arbetstid för lönebetäckning
         * och måste vara rimlig enligt företagspolicy.
         */
        @DecimalMin(value = "0.0", message = "Körtid kan inte vara negativ")
        @DecimalMax(value = "12.0", message = "Körtid kan inte överstiga 12 timmar per dag")
        private BigDecimal driveTimeHours = BigDecimal.ZERO;

        // Konstruktorer
        public EmployeeTimeDto() {}

        public EmployeeTimeDto(Long employeeId, LocalTime startTime, LocalTime endTime,
                               Integer lunchMinutes, Boolean isDriver, BigDecimal driveTimeHours) {
            this.employeeId = employeeId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.lunchMinutes = lunchMinutes != null ? lunchMinutes : 0;
            this.isDriver = isDriver != null ? isDriver : false;
            this.driveTimeHours = (this.isDriver && driveTimeHours != null) ? driveTimeHours : BigDecimal.ZERO;
        }

        // Bekvämlighets-konstruktor för vanliga fall (ingen körning)
        public EmployeeTimeDto(Long employeeId, LocalTime startTime, LocalTime endTime, Integer lunchMinutes) {
            this(employeeId, startTime, endTime, lunchMinutes, false, BigDecimal.ZERO);
        }

        /**
         * Beräknar total arbetstid för denna medarbetare (inklusive körtid).
         *
         * Denna metod duplicerar logik från EmployeeTime-entiteten men gör det
         * möjligt att validera arbetstid redan på DTO-nivå för bättre error handling.
         */
        public BigDecimal calculateTotalHours() {
            if (startTime == null || endTime == null) {
                return BigDecimal.ZERO;
            }

            // Beräkna ordinarie arbetstid minus lunch
            long workMinutes = java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime);
            long lunchMins = lunchMinutes != null ? lunchMinutes : 0;
            long netWorkMinutes = workMinutes - lunchMins;

            BigDecimal workHours = BigDecimal.valueOf(netWorkMinutes)
                    .divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);

            // Lägg till körtid om förare
            if (Boolean.TRUE.equals(isDriver) && driveTimeHours != null) {
                workHours = workHours.add(driveTimeHours);
            }

            return workHours.max(BigDecimal.ZERO);
        }

        /**
         * Validerar att denna arbetstidsregistrering följer grundläggande affärsregler.
         *
         * Komplettera field-level validation med business logic som kräver
         * koordination mellan flera fält.
         */
        public boolean isValidWorkTime() {
            if (startTime == null || endTime == null) return false;
            if (!endTime.isAfter(startTime)) return false;

            // Kontrollera att körtid bara finns för förare
            if (!Boolean.TRUE.equals(isDriver) && driveTimeHours != null &&
                    driveTimeHours.compareTo(BigDecimal.ZERO) > 0) {
                return false;
            }

            // Kontrollera att lunchtid inte är längre än arbetstid
            long totalWorkMinutes = java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime);
            if (lunchMinutes != null && lunchMinutes > totalWorkMinutes) {
                return false;
            }

            return true;
        }

        // Getters och setters
        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

        public Integer getLunchMinutes() { return lunchMinutes; }
        public void setLunchMinutes(Integer lunchMinutes) { this.lunchMinutes = lunchMinutes; }

        public Boolean getIsDriver() { return isDriver; }
        public void setIsDriver(Boolean isDriver) {
            this.isDriver = isDriver;
            // Nollställ körtid om inte längre förare
            if (!Boolean.TRUE.equals(isDriver)) {
                this.driveTimeHours = BigDecimal.ZERO;
            }
        }

        public BigDecimal getDriveTimeHours() { return driveTimeHours; }
        public void setDriveTimeHours(BigDecimal driveTimeHours) {
            // Körtid kan bara sättas för förare
            if (Boolean.TRUE.equals(isDriver) && driveTimeHours != null) {
                this.driveTimeHours = driveTimeHours;
            } else {
                this.driveTimeHours = BigDecimal.ZERO;
            }
        }

        @Override
        public String toString() {
            return "EmployeeTimeDto{" +
                    "employeeId=" + employeeId +
                    ", startTime=" + startTime +
                    ", endTime=" + endTime +
                    ", lunchMinutes=" + lunchMinutes +
                    ", isDriver=" + isDriver +
                    ", driveTimeHours=" + driveTimeHours +
                    ", totalHours=" + calculateTotalHours() +
                    '}';
        }
    }

    /**
     * DTO för utrustningsanvändning på arbetsdagen.
     *
     * Representerar användning av en specifik typ av utrustning inklusive
     * kvantitet och eventuella speciella noter om användningen.
     */
    public static class EquipmentUsageDto {

        /**
         * ID för utrustningen som användes.
         */
        @NotNull(message = "Utrustning måste anges")
        @Positive(message = "Utrustnings-ID måste vara ett positivt tal")
        private Long equipmentId;

        /**
         * Antal enheter av denna utrustning som användes.
         */
        @NotNull(message = "Kvantitet måste anges")
        @Min(value = 1, message = "Kvantitet måste vara minst 1")
        @Max(value = 10, message = "Maximum 10 enheter per utrustningstyp per dag")
        private Integer quantity = 1;

        /**
         * Valfria noter om användningen av denna utrustning.
         *
         * Kan innehålla information om skador, specialanvändning,
         * eller andra detaljer som är relevanta för kostnadsspårning.
         */
        @Size(max = 500, message = "Utrustningsnotes får inte vara längre än 500 tecken")
        private String notes;

        // Konstruktorer
        public EquipmentUsageDto() {}

        public EquipmentUsageDto(Long equipmentId, Integer quantity, String notes) {
            this.equipmentId = equipmentId;
            this.quantity = quantity != null ? quantity : 1;
            this.notes = notes;
        }

        // Bekvämlighets-konstruktor för vanliga fall
        public EquipmentUsageDto(Long equipmentId, Integer quantity) {
            this(equipmentId, quantity, null);
        }

        // Getters och setters
        public Long getEquipmentId() { return equipmentId; }
        public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        @Override
        public String toString() {
            return "EquipmentUsageDto{" +
                    "equipmentId=" + equipmentId +
                    ", quantity=" + quantity +
                    ", notes='" + notes + '\'' +
                    '}';
        }
    }

    // =================================================================
    // HUVUDKLASS KONSTRUKTORER OCH METODER
    // =================================================================

    // Default konstruktor för JSON deserialization
    public CreateWorkDayDto() {}

    /**
     * Komplett konstruktor för programmatisk användning.
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
    // BUSINESS LOGIC METODER - DTO-NIVÅ VALIDERING
    // =================================================================

    /**
     * Beräknar total arbetstid för alla medarbetare på denna arbetsdag.
     *
     * Användbart för snabb validering och användarfeedback innan
     * kompletta serverside-validering körs.
     */
    public BigDecimal calculateTotalWorkHours() {
        return employeeTimes.stream()
                .map(EmployeeTimeDto::calculateTotalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Räknar antal unika medarbetare för snabb duplikatdetektering.
     */
    public long getUniqueEmployeeCount() {
        return employeeTimes.stream()
                .map(EmployeeTimeDto::getEmployeeId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
    }

    /**
     * Kontrollerar om det finns dubbletter av medarbetare i listan.
     */
    public boolean hasDuplicateEmployees() {
        return getUniqueEmployeeCount() < employeeTimes.size();
    }

    /**
     * Kontrollerar om det finns dubbletter av utrustning i listan.
     */
    public boolean hasDuplicateEquipment() {
        long uniqueEquipmentCount = equipmentUsage.stream()
                .map(EquipmentUsageDto::getEquipmentId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        return uniqueEquipmentCount < equipmentUsage.size();
    }

    /**
     * Validerar grundläggande affärslogik på DTO-nivå.
     *
     * Denna metod kompletterar field-level validation med business rules
     * som kräver koordination mellan collections och fält.
     */
    public boolean isValidBusinessLogic() {
        // Kontrollera dubbletter
        if (hasDuplicateEmployees() || hasDuplicateEquipment()) {
            return false;
        }

        // Validera alla arbetstider
        for (EmployeeTimeDto employeeTime : employeeTimes) {
            if (!employeeTime.isValidWorkTime()) {
                return false;
            }
        }

        // Kontrollera rimlig total arbetstid
        BigDecimal totalHours = calculateTotalWorkHours();
        if (totalHours.compareTo(new BigDecimal("200")) > 0) { // Max 200 timmar total per dag
            return false;
        }

        return true;
    }

    /**
     * Skapar en användarvänlig sammanfattning av arbetsdagen.
     *
     * Användbart för confirmation-meddelanden och logging.
     */
    public String getWorkDaySummary() {
        BigDecimal totalHours = calculateTotalWorkHours();
        int employeeCount = employeeTimes.size();
        int equipmentCount = equipmentUsage.size();

        return String.format("Arbetsdag %s: %d medarbetare, %.1f timmar totalt%s",
                date,
                employeeCount,
                totalHours.doubleValue(),
                equipmentCount > 0 ? ", " + equipmentCount + " utrustningsobjekt" : "");
    }

    // =================================================================
    // CONVENIENCE METODER FÖR COLLECTION MANAGEMENT
    // =================================================================

    /**
     * Lägger till en medarbetares arbetstid till arbetsdagen.
     */
    public void addEmployeeTime(EmployeeTimeDto employeeTime) {
        if (employeeTime != null) {
            this.employeeTimes.add(employeeTime);
        }
    }

    /**
     * Lägger till utrustningsanvändning till arbetsdagen.
     */
    public void addEquipmentUsage(EquipmentUsageDto equipmentUsage) {
        if (equipmentUsage != null) {
            this.equipmentUsage.add(equipmentUsage);
        }
    }

    // =================================================================
    // STANDARD GETTERS OCH SETTERS
    // =================================================================

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getSupervisorId() { return supervisorId; }
    public void setSupervisorId(Long supervisorId) { this.supervisorId = supervisorId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<EmployeeTimeDto> getEmployeeTimes() { return employeeTimes; }
    public void setEmployeeTimes(List<EmployeeTimeDto> employeeTimes) {
        this.employeeTimes = employeeTimes != null ? employeeTimes : new ArrayList<>();
    }

    public List<EquipmentUsageDto> getEquipmentUsage() { return equipmentUsage; }
    public void setEquipmentUsage(List<EquipmentUsageDto> equipmentUsage) {
        this.equipmentUsage = equipmentUsage != null ? equipmentUsage : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "CreateWorkDayDto{" +
                "date=" + date +
                ", taskId=" + taskId +
                ", supervisorId=" + supervisorId +
                ", notes='" + notes + '\'' +
                ", employeeCount=" + employeeTimes.size() +
                ", equipmentCount=" + equipmentUsage.size() +
                ", totalHours=" + calculateTotalWorkHours() +
                ", summary='" + getWorkDaySummary() + '\'' +
                '}';
    }
}