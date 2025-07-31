package com.gardening.timemanagement.dto.request;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;


public class UpdateWorkDayDto {

    // =================================================================
    // CONCURRENCY CONTROL - OPTIMISTIC LOCKING
    // =================================================================

    /**
     * Version av WorkDay-entiteten som denna uppdatering baseras på.
     *
     * Detta är grunden för optimistic locking - om version-numret inte
     * matchar current state i databasen betyder det att någon annan
     * har modifierat entiteten sedan denna klient hämtade den.
     *
     * Pattern: Concurrent Modification Detection
     * - Client läser WorkDay med version N
     * - Client skickar update med expectedVersion = N
     * - Server kontrollerar att current version fortfarande är N
     * - Om inte: reject update med "concurrent modification" error
     * - Om ja: apply update och increment version till N+1
     */
    @NotNull(message = "Version måste anges för säker uppdatering")
    @Positive(message = "Version måste vara ett positivt tal")
    private Long expectedVersion;

    /**
     * Timestamp för när klienten senast hämtade denna WorkDay.
     *
     * Kompletterar version-baserad locking med temporal validation
     * för att detektera "stale data" scenarios där klienten arbetar
     * med mycket gammal data.
     */
    @NotNull(message = "Senaste hämtningstid måste anges")
    @PastOrPresent(message = "Senaste hämtningstid kan inte vara i framtiden")
    private LocalDateTime lastFetchedAt;

    /**
     * Orsak till denna uppdatering för audit trail.
     *
     * Hjälper med troubleshooting och compliance genom att dokumentera
     * varför förändringar gjordes, inte bara vad som ändrades.
     */
    @Size(max = 500, message = "Uppdateringsorsak får inte överstiga 500 tecken")
    private String updateReason;

    // =================================================================
    // PARTIAL UPDATE FIELDS - NULL SEMANTICS
    // =================================================================

    /**
     * Nytt datum för arbetsdagen (null = ingen förändring).
     *
     * Datum-förändringar är särskilt känsliga eftersom de kan påverka
     * rapporter och löneberäkningar som redan blivit fastställda.
     * Validation måste kontrollera att förändringen är legal enligt
     * företagspolicy för retrospektiva ändringar.
     */
    private LocalDate date;

    /**
     * Nytt uppdrag för arbetsdagen (null = ingen förändring).
     *
     * VARNING: Att byta uppdrag för en befintlig arbetsdag kan ha
     * omfattande konsekvenser för kostnadsspårning och projektrapporter.
     * Denna förändring kräver ofta speciell authorization och audit logging.
     */
    private Long taskId;

    /**
     * Ny arbetsledare för arbetsdagen (null = ingen förändring).
     *
     * Speciellt värde: -1L betyder "ta bort arbetsledare" (sätt till null)
     * Detta mönster tillåter oss att distinguish mellan "ändra inte" (null)
     * och "ta bort värde" (-1L) i vår partial update semantics.
     */
    private Long supervisorId;

    /**
     * Nya anteckningar för arbetsdagen (null = ingen förändring).
     *
     * Speciellt värde: tom sträng "" betyder "rensa anteckningar"
     * Detta följer samma pattern som supervisorId för att hantera
     * skillnaden mellan "ändra inte" och "rensa värde".
     */
    @Size(max = 2000, message = "Anteckningar får inte överstiga 2000 tecken")
    private String notes;

    // =================================================================
    // COLLECTION UPDATE STRATEGIES - DELTA OPERATIONS
    // =================================================================

    /**
     * Strategy för hur employee times ska uppdateras.
     *
     * Detta enum-värde styr hur systemet tolkar innehållet i
     * employeeTimeUpdates listan och är kritiskt för att förhindra
     * oavsiktlig data loss vid collection updates.
     */
    private CollectionUpdateStrategy employeeTimeStrategy = CollectionUpdateStrategy.NO_CHANGE;

    /**
     * Lista över employee time operations att utföra.
     *
     * Innehållet tolkas enligt employeeTimeStrategy:
     * - REPLACE_ALL: Lista representerar komplett ny collection
     * - ADD_OR_UPDATE: Lista innehåller endast element att lägga till/uppdatera
     * - EXPLICIT_OPERATIONS: Varje element har operation type specifierat
     */
    @Valid
    private List<EmployeeTimeUpdateDto> employeeTimeUpdates = new ArrayList<>();

    /**
     * Strategy för hur equipment usage ska uppdateras.
     */
    private CollectionUpdateStrategy equipmentStrategy = CollectionUpdateStrategy.NO_CHANGE;

    /**
     * Lista över equipment operations att utföra.
     */
    @Valid
    private List<EquipmentUsageUpdateDto> equipmentUpdates = new ArrayList<>();

    // =================================================================
    // SAFETY FLAGS - EXPLICIT DANGEROUS OPERATIONS
    // =================================================================

    /**
     * Explicit bekräftelse för operations som kan ha stora konsekvenser.
     *
     * Vissa updates är så potentiellt destructive att vi kräver explicit
     * acknowledgment från klienten att de förstår konsekvenserna.
     */
    private boolean confirmDestructiveChanges = false;

    /**
     * Om true, tillåt updates även om de bryter vissa business rules.
     *
     * Detta är en "emergency escape hatch" för administrativa corrections
     * som kan behöva bryta normal business logic. Kräver special permissions
     * och genererar extra audit logging.
     */
    private boolean overrideBusinessRules = false;

    /**
     * Om true, force update även vid concurrent modification warnings.
     *
     * Farlig operation som kan overwrite andra användares ändringar.
     * Bör endast användas av administratörer efter noggrann review.
     */
    private boolean forceConcurrentUpdate = false;

    // =================================================================
    // COLLECTION UPDATE STRATEGY ENUM
    // =================================================================

    /**
     * Strategies för hur collection updates ska tolkas och utföras.
     *
     * Denna enum är central för att förhindra accidental data loss
     * vid uppdatering av komplexa collections som employee times.
     */
    public enum CollectionUpdateStrategy {
        /**
         * Ingen förändring - collection lämnas orörd.
         * Default strategy för säkerhet.
         */
        NO_CHANGE,

        /**
         * Ersätt hela collection med ny data.
         *
         * VARNING: Denna strategy raderar all befintlig data i collection
         * och ersätter den med supplied data. Kräver confirmDestructiveChanges.
         */
        REPLACE_ALL,

        /**
         * Lägg till nya element eller uppdatera befintliga.
         *
         * Befintliga element som inte finns i update-listan lämnas orörd.
         * Säkrare än REPLACE_ALL men kan inte ta bort element.
         */
        ADD_OR_UPDATE,

        /**
         * Explicit operations - varje element har operation type.
         *
         * Mest flexibel strategy där varje EmployeeTimeUpdateDto eller
         * EquipmentUsageUpdateDto specificerar om den ska ADD, UPDATE eller DELETE.
         */
        EXPLICIT_OPERATIONS
    }

    // =================================================================
    // NESTED UPDATE DTOS - ELEMENT-LEVEL CHANGE SPECIFICATIONS
    // =================================================================

    /**
     * DTO för uppdatering av en medarbetares arbetstid.
     *
     * Kan representera antingen komplett ny data (för ADD operations)
     * eller partiella förändringar (för UPDATE operations).
     */
    public static class EmployeeTimeUpdateDto {

        /**
         * Operation type för denna employee time.
         * Endast relevant när EXPLICIT_OPERATIONS strategy används.
         */
        private OperationType operation = OperationType.UPDATE;

        /**
         * ID för befintlig EmployeeTime (null för nya entries).
         *
         * För UPDATE och DELETE operations måste detta ID matcha
         * en befintlig EmployeeTime i WorkDay. För ADD operations
         * ska detta vara null.
         */
        private Long employeeTimeId;

        /**
         * ID för medarbetaren (null = ingen förändring för updates).
         */
        @Positive(message = "Medarbetar-ID måste vara positivt om angivet")
        private Long employeeId;

        /**
         * Starttid (null = ingen förändring för updates).
         */
        private LocalTime startTime;

        /**
         * Sluttid (null = ingen förändring för updates).
         */
        private LocalTime endTime;

        /**
         * Lunchtid i minuter (null = ingen förändring för updates).
         */
        @Min(value = 0, message = "Lunchtid kan inte vara negativ")
        @Max(value = 120, message = "Lunchtid kan inte överstiga 120 minuter")
        private Integer lunchMinutes;

        /**
         * Förare-status (null = ingen förändring för updates).
         */
        private Boolean isDriver;

        /**
         * Körtid i timmar (null = ingen förändring för updates).
         */
        @DecimalMin(value = "0.0", message = "Körtid kan inte vara negativ")
        @DecimalMax(value = "12.0", message = "Körtid kan inte överstiga 12 timmar")
        private BigDecimal driveTimeHours;

        /**
         * Orsak till denna specifika förändring för audit trail.
         */
        @Size(max = 200, message = "Ändringsorsak får inte överstiga 200 tecken")
        private String changeReason;

        // Konstruktorer
        public EmployeeTimeUpdateDto() {}

        /**
         * Konstruktor för ADD operations.
         */
        public EmployeeTimeUpdateDto(Long employeeId, LocalTime startTime, LocalTime endTime,
                                     Integer lunchMinutes, Boolean isDriver, BigDecimal driveTimeHours) {
            this.operation = OperationType.ADD;
            this.employeeId = employeeId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.lunchMinutes = lunchMinutes;
            this.isDriver = isDriver;
            this.driveTimeHours = driveTimeHours;
        }

        /**
         * Konstruktor för UPDATE operations.
         */
        public EmployeeTimeUpdateDto(Long employeeTimeId, String changeReason) {
            this.operation = OperationType.UPDATE;
            this.employeeTimeId = employeeTimeId;
            this.changeReason = changeReason;
        }

        /**
         * Factory method för DELETE operations.
         */
        public static EmployeeTimeUpdateDto deleteOperation(Long employeeTimeId, String reason) {
            EmployeeTimeUpdateDto dto = new EmployeeTimeUpdateDto();
            dto.operation = OperationType.DELETE;
            dto.employeeTimeId = employeeTimeId;
            dto.changeReason = reason;
            return dto;
        }

        /**
         * Validerar att denna update är konsistent med sin operation type.
         */
        public boolean isValidForOperation() {
            switch (operation) {
                case ADD:
                    // ADD kräver alla mandatory fields men inte ID
                    return employeeTimeId == null && employeeId != null &&
                            startTime != null && endTime != null;

                case UPDATE:
                    // UPDATE kräver ID men kan ha partial fields
                    return employeeTimeId != null;

                case DELETE:
                    // DELETE kräver endast ID
                    return employeeTimeId != null;

                default:
                    return false;
            }
        }

        // Getters och setters
        public OperationType getOperation() { return operation; }
        public void setOperation(OperationType operation) { this.operation = operation; }

        public Long getEmployeeTimeId() { return employeeTimeId; }
        public void setEmployeeTimeId(Long employeeTimeId) { this.employeeTimeId = employeeTimeId; }

        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

        public Integer getLunchMinutes() { return lunchMinutes; }
        public void setLunchMinutes(Integer lunchMinutes) { this.lunchMinutes = lunchMinutes; }

        public Boolean getIsDriver() { return isDriver; }
        public void setIsDriver(Boolean isDriver) { this.isDriver = isDriver; }

        public BigDecimal getDriveTimeHours() { return driveTimeHours; }
        public void setDriveTimeHours(BigDecimal driveTimeHours) { this.driveTimeHours = driveTimeHours; }

        public String getChangeReason() { return changeReason; }
        public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

        @Override
        public String toString() {
            return "EmployeeTimeUpdateDto{" +
                    "operation=" + operation +
                    ", employeeTimeId=" + employeeTimeId +
                    ", employeeId=" + employeeId +
                    ", changeReason='" + changeReason + '\'' +
                    '}';
        }
    }

    /**
     * DTO för uppdatering av utrustningsanvändning.
     *
     * Följer samma mönster som EmployeeTimeUpdateDto men för equipment.
     */
    public static class EquipmentUsageUpdateDto {

        private OperationType operation = OperationType.UPDATE;
        private Long equipmentUsageId;

        @Positive(message = "Utrustnings-ID måste vara positivt om angivet")
        private Long equipmentId;

        @Min(value = 1, message = "Kvantitet måste vara minst 1")
        @Max(value = 10, message = "Kvantitet kan inte överstiga 10")
        private Integer quantity;

        @Size(max = 500, message = "Noter får inte överstiga 500 tecken")
        private String notes;

        @Size(max = 200, message = "Ändringsorsak får inte överstiga 200 tecken")
        private String changeReason;

        // Konstruktorer och metoder följer samma pattern som EmployeeTimeUpdateDto
        public EquipmentUsageUpdateDto() {}

        public static EquipmentUsageUpdateDto deleteOperation(Long equipmentUsageId, String reason) {
            EquipmentUsageUpdateDto dto = new EquipmentUsageUpdateDto();
            dto.operation = OperationType.DELETE;
            dto.equipmentUsageId = equipmentUsageId;
            dto.changeReason = reason;
            return dto;
        }

        public boolean isValidForOperation() {
            switch (operation) {
                case ADD:
                    return equipmentUsageId == null && equipmentId != null && quantity != null;
                case UPDATE:
                    return equipmentUsageId != null;
                case DELETE:
                    return equipmentUsageId != null;
                default:
                    return false;
            }
        }

        // Getters och setters (förenklade för brevity)
        public OperationType getOperation() { return operation; }
        public void setOperation(OperationType operation) { this.operation = operation; }

        public Long getEquipmentUsageId() { return equipmentUsageId; }
        public void setEquipmentUsageId(Long equipmentUsageId) { this.equipmentUsageId = equipmentUsageId; }

        public Long getEquipmentId() { return equipmentId; }
        public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getChangeReason() { return changeReason; }
        public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    }

    // =================================================================
    // OPERATION TYPE ENUM
    // =================================================================

    /**
     * Types av operations som kan utföras på collection elements.
     */
    public enum OperationType {
        ADD,    // Lägg till nytt element
        UPDATE, // Uppdatera befintligt element
        DELETE  // Ta bort befintligt element
    }

    // =================================================================
    // KONSTRUKTORER OCH UTILITY METODER
    // =================================================================

    public UpdateWorkDayDto() {}

    /**
     * Konstruktor för enkel uppdatering av grundläggande fält.
     */
    public UpdateWorkDayDto(Long expectedVersion, LocalDateTime lastFetchedAt, String updateReason) {
        this.expectedVersion = expectedVersion;
        this.lastFetchedAt = lastFetchedAt;
        this.updateReason = updateReason;
    }

    /**
     * Kontrollerar om denna update innehåller destructive operations.
     *
     * Destructive operations kräver extra validation och confirmDestructiveChanges.
     */
    public boolean hasDestructiveOperations() {
        // Task ID changes är destructive
        if (taskId != null) return true;

        // Collection replacements är destructive
        if (employeeTimeStrategy == CollectionUpdateStrategy.REPLACE_ALL ||
                equipmentStrategy == CollectionUpdateStrategy.REPLACE_ALL) {
            return true;
        }

        // DELETE operations är destructive
        return employeeTimeUpdates.stream().anyMatch(u -> u.getOperation() == OperationType.DELETE) ||
                equipmentUpdates.stream().anyMatch(u -> u.getOperation() == OperationType.DELETE);
    }

    /**
     * Kontrollerar om denna update kräver special authorization.
     */
    public boolean requiresSpecialAuthorization() {
        return hasDestructiveOperations() || overrideBusinessRules || forceConcurrentUpdate;
    }

    /**
     * Genererar en sammanfattning av vilka förändringar som kommer att göras.
     *
     * Användbart för confirmation dialogs och audit logging.
     */
    public String getChangeSummary() {
        StringBuilder summary = new StringBuilder();

        if (date != null) summary.append("Datum kommer att ändras. ");
        if (taskId != null) summary.append("Uppdrag kommer att ändras. ");
        if (supervisorId != null) {
            if (supervisorId == -1L) {
                summary.append("Arbetsledare kommer att tas bort. ");
            } else {
                summary.append("Arbetsledare kommer att ändras. ");
            }
        }
        if (notes != null) summary.append("Anteckningar kommer att uppdateras. ");

        if (employeeTimeStrategy != CollectionUpdateStrategy.NO_CHANGE) {
            summary.append(String.format("Medarbetartider kommer att uppdateras (%s). ",
                    employeeTimeStrategy.name()));
        }

        if (equipmentStrategy != CollectionUpdateStrategy.NO_CHANGE) {
            summary.append(String.format("Utrustning kommer att uppdateras (%s). ",
                    equipmentStrategy.name()));
        }

        return summary.toString().trim();
    }

    // =================================================================
    // GETTERS OCH SETTERS
    // =================================================================

    public Long getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(Long expectedVersion) { this.expectedVersion = expectedVersion; }

    public LocalDateTime getLastFetchedAt() { return lastFetchedAt; }
    public void setLastFetchedAt(LocalDateTime lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; }

    public String getUpdateReason() { return updateReason; }
    public void setUpdateReason(String updateReason) { this.updateReason = updateReason; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getSupervisorId() { return supervisorId; }
    public void setSupervisorId(Long supervisorId) { this.supervisorId = supervisorId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public CollectionUpdateStrategy getEmployeeTimeStrategy() { return employeeTimeStrategy; }
    public void setEmployeeTimeStrategy(CollectionUpdateStrategy employeeTimeStrategy) {
        this.employeeTimeStrategy = employeeTimeStrategy;
    }

    public List<EmployeeTimeUpdateDto> getEmployeeTimeUpdates() { return employeeTimeUpdates; }
    public void setEmployeeTimeUpdates(List<EmployeeTimeUpdateDto> employeeTimeUpdates) {
        this.employeeTimeUpdates = employeeTimeUpdates != null ? employeeTimeUpdates : new ArrayList<>();
    }

    public CollectionUpdateStrategy getEquipmentStrategy() { return equipmentStrategy; }
    public void setEquipmentStrategy(CollectionUpdateStrategy equipmentStrategy) {
        this.equipmentStrategy = equipmentStrategy;
    }

    public List<EquipmentUsageUpdateDto> getEquipmentUpdates() { return equipmentUpdates; }
    public void setEquipmentUpdates(List<EquipmentUsageUpdateDto> equipmentUpdates) {
        this.equipmentUpdates = equipmentUpdates != null ? equipmentUpdates : new ArrayList<>();
    }

    public boolean isConfirmDestructiveChanges() { return confirmDestructiveChanges; }
    public void setConfirmDestructiveChanges(boolean confirmDestructiveChanges) {
        this.confirmDestructiveChanges = confirmDestructiveChanges;
    }

    public boolean isOverrideBusinessRules() { return overrideBusinessRules; }
    public void setOverrideBusinessRules(boolean overrideBusinessRules) {
        this.overrideBusinessRules = overrideBusinessRules;
    }

    public boolean isForceConcurrentUpdate() { return forceConcurrentUpdate; }
    public void setForceConcurrentUpdate(boolean forceConcurrentUpdate) {
        this.forceConcurrentUpdate = forceConcurrentUpdate;
    }

    @Override
    public String toString() {
        return "UpdateWorkDayDto{" +
                "expectedVersion=" + expectedVersion +
                ", hasChanges=" + !getChangeSummary().isEmpty() +
                ", isDestructive=" + hasDestructiveOperations() +
                ", requiresAuth=" + requiresSpecialAuthorization() +
                ", summary='" + getChangeSummary() + '\'' +
                '}';
    }
}