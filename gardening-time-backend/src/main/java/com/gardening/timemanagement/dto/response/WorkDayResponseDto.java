package com.gardening.timemanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

/**
 * Enterprise-nivå response DTO för WorkDay-entiteten med sofistikerad data-arkitektur.
 *
 * Denna DTO implementerar advanced response patterns för optimal performance och
 * användarvänlighet genom intelligent expansion strategies och embedded data structures.
 *
 * Arkitektoniska principer:
 * - Conditional expansion för att balansera completeness mot performance
 * - Embedded related entity data för att minimera API round-trips
 * - Pre-calculated business metrics för optimal client-side experience
 * - Rich metadata för intelligent client-side caching och optimization
 *
 * Designphilosophy: "Provide exactly what clients need, when they need it,
 * without unnecessary complexity or performance overhead."
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkDayResponseDto {

    // =================================================================
    // CORE WORKDAY METADATA - ALLTID INKLUDERAT
    // =================================================================

    /**
     * Unikt ID för denna arbetsdag.
     */
    private Long id;

    /**
     * Datum för arbetsdagen.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * Version för optimistic locking och change detection.
     */
    private Long version;

    /**
     * Valfria anteckningar för arbetsdagen.
     */
    private String notes;

    /**
     * Timestamps för audit trail.
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // =================================================================
    // EMBEDDED TASK AND CUSTOMER DATA - PERFORMANCE OPTIMIZATION
    // =================================================================

    /**
     * Embedded task information för att undvika separata API-anrop.
     *
     * Detta är en kritisk performance-optimering eftersom nästan alla
     * användningsfall av WorkDay behöver basic task information.
     * Genom att embedda denna data sparar vi potentiellt tusentals
     * API-anrop i list views.
     */
    private TaskSummaryDto task;

    /**
     * Embedded customer information via task för ytterligare optimering.
     *
     * Customer data är så ofta använd att vi inkluderar den direkt
     * istället för att tvinga klienten att följa task.customerId länken.
     */
    private CustomerSummaryDto customer;

    /**
     * Embedded supervisor information när tillgängligt.
     *
     * Null om ingen supervisor är tilldelad. Inkluderar basic info
     * för att undvika extra API-anrop för vanliga användningsfall.
     */
    private EmployeeSummaryDto supervisor;

    // =================================================================
    // EMPLOYEE TIMES - CONDITIONAL EXPANSION STRATEGY
    // =================================================================

    /**
     * Lista över detaljerade arbetstider för alla medarbetare.
     *
     * Detta är potentiellt den största delen av response, så vi använder
     * intelligent loading strategies baserat på användningsfall.
     *
     * För list views: begränsad till basic summary information
     * För detail views: full information med alla beräknade fält
     * För edit views: inkluderar även metadata för optimistic locking
     */
    private List<EmployeeTimeDetailDto> employeeTimes = new ArrayList<>();

    /**
     * Condensed summary av employee times för performance-kritiska användningsfall.
     *
     * När full employeeTimes data inte behövs kan denna summary användas
     * för att visa basic information utan stor performance impact.
     */
    private EmployeeTimeSummaryDto employeeTimeSummary;

    // =================================================================
    // EQUIPMENT USAGE - STREAMLINED FOR COST TRACKING
    // =================================================================

    /**
     * Lista över utrustning som användes denna dag.
     *
     * Optimerad för kostnadsspårning och resursplanering med
     * embedded pricing information för att undvika extra lookups.
     */
    private List<EquipmentUsageDetailDto> equipmentUsage = new ArrayList<>();

    /**
     * Aggregerad kostnadsinformation för all utrustning.
     */
    private EquipmentCostSummaryDto equipmentCostSummary;

    // =================================================================
    // CALCULATED FIELDS - SERVER-SIDE BUSINESS INTELLIGENCE
    // =================================================================

    /**
     * Pre-calculated metrics för att minska klient-side processing.
     *
     * Dessa fält beräknas server-side och cachas i response för att
     * ge klienter omedelbar access till viktiga business metrics
     * utan behov av komplex frontend-logic.
     */
    private WorkDayMetricsDto metrics;

    /**
     * Status information som hjälper klienter förstå arbetsdagens tillstånd.
     */
    private WorkDayStatusDto status;

    // =================================================================
    // RESPONSE METADATA - CLIENT OPTIMIZATION HINTS
    // =================================================================

    /**
     * Metadata som hjälper klienter optimera sina requests och caching.
     *
     * Detta inkluderar information om vilka expansion options som
     * användes för denna response och hints om optimal caching strategies.
     */
    private ResponseMetadataDto metadata;

    // =================================================================
    // NESTED DTO CLASSES - EMBEDDED DATA STRUCTURES
    // =================================================================

    /**
     * Condensed task information optimerad för embedding.
     */
    public static class TaskSummaryDto {
        private Long id;
        private String number;
        private String status;
        private String description;

        // Konstruktorer
        public TaskSummaryDto() {}

        public TaskSummaryDto(Long id, String number, String status, String description) {
            this.id = id;
            this.number = number;
            this.status = status;
            this.description = description;
        }

        // Getters och setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getNumber() { return number; }
        public void setNumber(String number) { this.number = number; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * Condensed customer information via task embedding.
     */
    public static class CustomerSummaryDto {
        private Long id;
        private String name;
        private String phone;

        public CustomerSummaryDto() {}

        public CustomerSummaryDto(Long id, String name, String phone) {
            this.id = id;
            this.name = name;
            this.phone = phone;
        }

        // Getters och setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    /**
     * Condensed employee information för supervisor och summary contexts.
     */
    public static class EmployeeSummaryDto {
        private Long id;
        private String name;
        private String phone;
        private Boolean isActive;

        public EmployeeSummaryDto() {}

        public EmployeeSummaryDto(Long id, String name, String phone, Boolean isActive) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.isActive = isActive;
        }

        // Getters och setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }

    /**
     * Detaljerad arbetstidsinformation för en medarbetare.
     *
     * Inkluderar både raw data och calculated fields för optimal
     * klient-side rendering utan extra processing.
     */
    public static class EmployeeTimeDetailDto {
        private Long id;
        private EmployeeSummaryDto employee;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;

        private Integer lunchMinutes;
        private Boolean isDriver;

        @JsonProperty("driveTimeHours")
        private BigDecimal driveTimeHours;

        // Calculated fields för klient convenience
        @JsonProperty("totalHours")
        private BigDecimal totalHours;

        @JsonProperty("regularHours")
        private BigDecimal regularHours;

        @JsonProperty("workTimeDescription")
        private String workTimeDescription;

        // Konstruktorer
        public EmployeeTimeDetailDto() {}

        public EmployeeTimeDetailDto(Long id, EmployeeSummaryDto employee,
                                     LocalTime startTime, LocalTime endTime,
                                     Integer lunchMinutes, Boolean isDriver,
                                     BigDecimal driveTimeHours, BigDecimal totalHours,
                                     BigDecimal regularHours, String workTimeDescription) {
            this.id = id;
            this.employee = employee;
            this.startTime = startTime;
            this.endTime = endTime;
            this.lunchMinutes = lunchMinutes;
            this.isDriver = isDriver;
            this.driveTimeHours = driveTimeHours;
            this.totalHours = totalHours;
            this.regularHours = regularHours;
            this.workTimeDescription = workTimeDescription;
        }

        // Getters och setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public EmployeeSummaryDto getEmployee() { return employee; }
        public void setEmployee(EmployeeSummaryDto employee) { this.employee = employee; }

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

        public BigDecimal getTotalHours() { return totalHours; }
        public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }

        public BigDecimal getRegularHours() { return regularHours; }
        public void setRegularHours(BigDecimal regularHours) { this.regularHours = regularHours; }

        public String getWorkTimeDescription() { return workTimeDescription; }
        public void setWorkTimeDescription(String workTimeDescription) { this.workTimeDescription = workTimeDescription; }
    }

    /**
     * Aggregerad summary av alla arbetstider för performance-optimering.
     */
    public static class EmployeeTimeSummaryDto {
        private Integer employeeCount;
        private BigDecimal totalHours;
        private BigDecimal totalDriveHours;
        private BigDecimal averageHoursPerEmployee;
        private List<String> employeeNames;

        public EmployeeTimeSummaryDto() {}

        public EmployeeTimeSummaryDto(Integer employeeCount, BigDecimal totalHours,
                                      BigDecimal totalDriveHours, BigDecimal averageHoursPerEmployee,
                                      List<String> employeeNames) {
            this.employeeCount = employeeCount;
            this.totalHours = totalHours;
            this.totalDriveHours = totalDriveHours;
            this.averageHoursPerEmployee = averageHoursPerEmployee;
            this.employeeNames = employeeNames;
        }

        // Getters och setters
        public Integer getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(Integer employeeCount) { this.employeeCount = employeeCount; }

        public BigDecimal getTotalHours() { return totalHours; }
        public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }

        public BigDecimal getTotalDriveHours() { return totalDriveHours; }
        public void setTotalDriveHours(BigDecimal totalDriveHours) { this.totalDriveHours = totalDriveHours; }

        public BigDecimal getAverageHoursPerEmployee() { return averageHoursPerEmployee; }
        public void setAverageHoursPerEmployee(BigDecimal averageHoursPerEmployee) { this.averageHoursPerEmployee = averageHoursPerEmployee; }

        public List<String> getEmployeeNames() { return employeeNames; }
        public void setEmployeeNames(List<String> employeeNames) { this.employeeNames = employeeNames; }
    }

    /**
     * Detaljerad utrustningsinformation med embedded pricing.
     */
    public static class EquipmentUsageDetailDto {
        private Long id;
        private EquipmentSummaryDto equipment;
        private Integer quantity;
        private String notes;

        // Calculated cost information
        private BigDecimal dailyPricePerUnit;
        private BigDecimal totalCost;
        private String costDescription;

        public EquipmentUsageDetailDto() {}

        public EquipmentUsageDetailDto(Long id, EquipmentSummaryDto equipment, Integer quantity,
                                       String notes, BigDecimal dailyPricePerUnit, BigDecimal totalCost,
                                       String costDescription) {
            this.id = id;
            this.equipment = equipment;
            this.quantity = quantity;
            this.notes = notes;
            this.dailyPricePerUnit = dailyPricePerUnit;
            this.totalCost = totalCost;
            this.costDescription = costDescription;
        }

        // Getters och setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public EquipmentSummaryDto getEquipment() { return equipment; }
        public void setEquipment(EquipmentSummaryDto equipment) { this.equipment = equipment; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public BigDecimal getDailyPricePerUnit() { return dailyPricePerUnit; }
        public void setDailyPricePerUnit(BigDecimal dailyPricePerUnit) { this.dailyPricePerUnit = dailyPricePerUnit; }

        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

        public String getCostDescription() { return costDescription; }
        public void setCostDescription(String costDescription) { this.costDescription = costDescription; }

        /**
         * Nested DTO för utrustningsinformation.
         */
        public static class EquipmentSummaryDto {
            private Long id;
            private String name;
            private BigDecimal dailyPrice;
            private Boolean isActive;

            public EquipmentSummaryDto() {}

            public EquipmentSummaryDto(Long id, String name, BigDecimal dailyPrice, Boolean isActive) {
                this.id = id;
                this.name = name;
                this.dailyPrice = dailyPrice;
                this.isActive = isActive;
            }

            // Getters och setters
            public Long getId() { return id; }
            public void setId(Long id) { this.id = id; }

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }

            public BigDecimal getDailyPrice() { return dailyPrice; }
            public void setDailyPrice(BigDecimal dailyPrice) { this.dailyPrice = dailyPrice; }

            public Boolean getIsActive() { return isActive; }
            public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        }
    }

    /**
     * Aggregerad kostnadsinformation för all utrustning.
     */
    public static class EquipmentCostSummaryDto {
        private Integer equipmentItemCount;
        private BigDecimal totalEquipmentCost;
        private List<String> equipmentNames;
        private String costBreakdown;

        public EquipmentCostSummaryDto() {}

        public EquipmentCostSummaryDto(Integer equipmentItemCount, BigDecimal totalEquipmentCost,
                                       List<String> equipmentNames, String costBreakdown) {
            this.equipmentItemCount = equipmentItemCount;
            this.totalEquipmentCost = totalEquipmentCost;
            this.equipmentNames = equipmentNames;
            this.costBreakdown = costBreakdown;
        }

        // Getters och setters
        public Integer getEquipmentItemCount() { return equipmentItemCount; }
        public void setEquipmentItemCount(Integer equipmentItemCount) { this.equipmentItemCount = equipmentItemCount; }

        public BigDecimal getTotalEquipmentCost() { return totalEquipmentCost; }
        public void setTotalEquipmentCost(BigDecimal totalEquipmentCost) { this.totalEquipmentCost = totalEquipmentCost; }

        public List<String> getEquipmentNames() { return equipmentNames; }
        public void setEquipmentNames(List<String> equipmentNames) { this.equipmentNames = equipmentNames; }

        public String getCostBreakdown() { return costBreakdown; }
        public void setCostBreakdown(String costBreakdown) { this.costBreakdown = costBreakdown; }
    }

    /**
     * Pre-calculated business metrics för denna arbetsdag.
     */
    public static class WorkDayMetricsDto {
        private BigDecimal totalLaborHours;
        private BigDecimal totalLaborCost;
        private BigDecimal totalEquipmentCost;
        private BigDecimal totalProjectCost;
        private BigDecimal averageProductivity;
        private String efficiencyRating;
        private String costEffectivenessRating;

        public WorkDayMetricsDto() {}

        public WorkDayMetricsDto(BigDecimal totalLaborHours, BigDecimal totalLaborCost,
                                 BigDecimal totalEquipmentCost, BigDecimal totalProjectCost,
                                 BigDecimal averageProductivity, String efficiencyRating,
                                 String costEffectivenessRating) {
            this.totalLaborHours = totalLaborHours;
            this.totalLaborCost = totalLaborCost;
            this.totalEquipmentCost = totalEquipmentCost;
            this.totalProjectCost = totalProjectCost;
            this.averageProductivity = averageProductivity;
            this.efficiencyRating = efficiencyRating;
            this.costEffectivenessRating = costEffectivenessRating;
        }

        // Getters och setters
        public BigDecimal getTotalLaborHours() { return totalLaborHours; }
        public void setTotalLaborHours(BigDecimal totalLaborHours) { this.totalLaborHours = totalLaborHours; }

        public BigDecimal getTotalLaborCost() { return totalLaborCost; }
        public void setTotalLaborCost(BigDecimal totalLaborCost) { this.totalLaborCost = totalLaborCost; }

        public BigDecimal getTotalEquipmentCost() { return totalEquipmentCost; }
        public void setTotalEquipmentCost(BigDecimal totalEquipmentCost) { this.totalEquipmentCost = totalEquipmentCost; }

        public BigDecimal getTotalProjectCost() { return totalProjectCost; }
        public void setTotalProjectCost(BigDecimal totalProjectCost) { this.totalProjectCost = totalProjectCost; }

        public BigDecimal getAverageProductivity() { return averageProductivity; }
        public void setAverageProductivity(BigDecimal averageProductivity) { this.averageProductivity = averageProductivity; }

        public String getEfficiencyRating() { return efficiencyRating; }
        public void setEfficiencyRating(String efficiencyRating) { this.efficiencyRating = efficiencyRating; }

        public String getCostEffectivenessRating() { return costEffectivenessRating; }
        public void setCostEffectivenessRating(String costEffectivenessRating) { this.costEffectivenessRating = costEffectivenessRating; }
    }

    /**
     * Status information för denna arbetsdag.
     */
    public static class WorkDayStatusDto {
        private String overallStatus;
        private Boolean isEditable;
        private Boolean hasValidationWarnings;
        private List<String> statusMessages;
        private String lastModifiedBy;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastModifiedAt;

        public WorkDayStatusDto() {}

        public WorkDayStatusDto(String overallStatus, Boolean isEditable, Boolean hasValidationWarnings,
                                List<String> statusMessages, String lastModifiedBy, LocalDateTime lastModifiedAt) {
            this.overallStatus = overallStatus;
            this.isEditable = isEditable;
            this.hasValidationWarnings = hasValidationWarnings;
            this.statusMessages = statusMessages;
            this.lastModifiedBy = lastModifiedBy;
            this.lastModifiedAt = lastModifiedAt;
        }

        // Getters och setters
        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

        public Boolean getIsEditable() { return isEditable; }
        public void setIsEditable(Boolean isEditable) { this.isEditable = isEditable; }

        public Boolean getHasValidationWarnings() { return hasValidationWarnings; }
        public void setHasValidationWarnings(Boolean hasValidationWarnings) { this.hasValidationWarnings = hasValidationWarnings; }

        public List<String> getStatusMessages() { return statusMessages; }
        public void setStatusMessages(List<String> statusMessages) { this.statusMessages = statusMessages; }

        public String getLastModifiedBy() { return lastModifiedBy; }
        public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }

        public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }
        public void setLastModifiedAt(LocalDateTime lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }
    }

    /**
     * Metadata om denna response för klient-optimering.
     */
    public static class ResponseMetadataDto {
        private String expansionLevel;
        private List<String> includedFields;
        private List<String> availableExpansions;
        private Integer cacheRecommendationSeconds;
        private String dataFreshness;

        public ResponseMetadataDto() {}

        public ResponseMetadataDto(String expansionLevel, List<String> includedFields,
                                   List<String> availableExpansions, Integer cacheRecommendationSeconds,
                                   String dataFreshness) {
            this.expansionLevel = expansionLevel;
            this.includedFields = includedFields;
            this.availableExpansions = availableExpansions;
            this.cacheRecommendationSeconds = cacheRecommendationSeconds;
            this.dataFreshness = dataFreshness;
        }

        // Getters och setters
        public String getExpansionLevel() { return expansionLevel; }
        public void setExpansionLevel(String expansionLevel) { this.expansionLevel = expansionLevel; }

        public List<String> getIncludedFields() { return includedFields; }
        public void setIncludedFields(List<String> includedFields) { this.includedFields = includedFields; }

        public List<String> getAvailableExpansions() { return availableExpansions; }
        public void setAvailableExpansions(List<String> availableExpansions) { this.availableExpansions = availableExpansions; }

        public Integer getCacheRecommendationSeconds() { return cacheRecommendationSeconds; }
        public void setCacheRecommendationSeconds(Integer cacheRecommendationSeconds) { this.cacheRecommendationSeconds = cacheRecommendationSeconds; }

        public String getDataFreshness() { return dataFreshness; }
        public void setDataFreshness(String dataFreshness) { this.dataFreshness = dataFreshness; }
    }

    // =================================================================
    // HUVUDKLASS KONSTRUKTORER OCH METODER
    // =================================================================

    public WorkDayResponseDto() {}

    /**
     * Konstruktor för basic responses (list views).
     */
    public WorkDayResponseDto(Long id, LocalDate date, Long version, TaskSummaryDto task,
                              CustomerSummaryDto customer, EmployeeTimeSummaryDto employeeTimeSummary) {
        this.id = id;
        this.date = date;
        this.version = version;
        this.task = task;
        this.customer = customer;
        this.employeeTimeSummary = employeeTimeSummary;
    }

    /**
     * Kontrollerar om denna response innehåller detaljerad information.
     */
    public boolean isDetailedResponse() {
        return employeeTimes != null && !employeeTimes.isEmpty();
    }

    /**
     * Kontrollerar om denna response innehåller cost information.
     */
    public boolean hasCostInformation() {
        return metrics != null && metrics.getTotalProjectCost() != null;
    }

    /**
     * Genererar en human-readable sammanfattning av arbetsdagen.
     */
    public String getWorkDaySummary() {
        StringBuilder summary = new StringBuilder();

        summary.append(String.format("Arbetsdag %s", date));

        if (customer != null) {
            summary.append(String.format(" för %s", customer.getName()));
        }

        if (employeeTimeSummary != null) {
            summary.append(String.format(" - %d medarbetare, %.1f timmar totalt",
                    employeeTimeSummary.getEmployeeCount(),
                    employeeTimeSummary.getTotalHours().doubleValue()));
        }

        if (equipmentCostSummary != null && equipmentCostSummary.getEquipmentItemCount() > 0) {
            summary.append(String.format(", %d utrustningsobjekt",
                    equipmentCostSummary.getEquipmentItemCount()));
        }

        return summary.toString();
    }

    /**
     * Beräknar total kostnad för arbetsdagen om information finns tillgänglig.
     */
    public BigDecimal getTotalCost() {
        if (metrics != null && metrics.getTotalProjectCost() != null) {
            return metrics.getTotalProjectCost();
        }

        // Fallback calculation om metrics inte är tillgängliga
        BigDecimal laborCost = BigDecimal.ZERO;
        BigDecimal equipmentCost = BigDecimal.ZERO;

        if (metrics != null) {
            if (metrics.getTotalLaborCost() != null) {
                laborCost = metrics.getTotalLaborCost();
            }
            if (metrics.getTotalEquipmentCost() != null) {
                equipmentCost = metrics.getTotalEquipmentCost();
            }
        }

        return laborCost.add(equipmentCost);
    }

    /**
     * Kontrollerar om arbetsdagen har några validation warnings.
     */
    public boolean hasWarnings() {
        return status != null && Boolean.TRUE.equals(status.getHasValidationWarnings());
    }

    /**
     * Kontrollerar om arbetsdagen kan redigeras.
     */
    public boolean isEditable() {
        return status != null && Boolean.TRUE.equals(status.getIsEditable());
    }

    /**
     * Returnerar rekommenderad cache-tid för denna response.
     */
    public int getRecommendedCacheSeconds() {
        if (metadata != null && metadata.getCacheRecommendationSeconds() != null) {
            return metadata.getCacheRecommendationSeconds();
        }

        // Default cache recommendations baserat på data type
        if (isDetailedResponse()) {
            return 300; // 5 minuter för detaljerade responses
        } else {
            return 900; // 15 minuter för summary responses
        }
    }

    // =================================================================
    // STANDARD GETTERS OCH SETTERS
    // =================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public TaskSummaryDto getTask() { return task; }
    public void setTask(TaskSummaryDto task) { this.task = task; }

    public CustomerSummaryDto getCustomer() { return customer; }
    public void setCustomer(CustomerSummaryDto customer) { this.customer = customer; }

    public EmployeeSummaryDto getSupervisor() { return supervisor; }
    public void setSupervisor(EmployeeSummaryDto supervisor) { this.supervisor = supervisor; }

    public List<EmployeeTimeDetailDto> getEmployeeTimes() { return employeeTimes; }
    public void setEmployeeTimes(List<EmployeeTimeDetailDto> employeeTimes) {
        this.employeeTimes = employeeTimes != null ? employeeTimes : new ArrayList<>();
    }

    public EmployeeTimeSummaryDto getEmployeeTimeSummary() { return employeeTimeSummary; }
    public void setEmployeeTimeSummary(EmployeeTimeSummaryDto employeeTimeSummary) {
        this.employeeTimeSummary = employeeTimeSummary;
    }

    public List<EquipmentUsageDetailDto> getEquipmentUsage() { return equipmentUsage; }
    public void setEquipmentUsage(List<EquipmentUsageDetailDto> equipmentUsage) {
        this.equipmentUsage = equipmentUsage != null ? equipmentUsage : new ArrayList<>();
    }

    public EquipmentCostSummaryDto getEquipmentCostSummary() { return equipmentCostSummary; }
    public void setEquipmentCostSummary(EquipmentCostSummaryDto equipmentCostSummary) {
        this.equipmentCostSummary = equipmentCostSummary;
    }

    public WorkDayMetricsDto getMetrics() { return metrics; }
    public void setMetrics(WorkDayMetricsDto metrics) { this.metrics = metrics; }

    public WorkDayStatusDto getStatus() { return status; }
    public void setStatus(WorkDayStatusDto status) { this.status = status; }

    public ResponseMetadataDto getMetadata() { return metadata; }
    public void setMetadata(ResponseMetadataDto metadata) { this.metadata = metadata; }

    @Override
    public String toString() {
        return "WorkDayResponseDto{" +
                "id=" + id +
                ", date=" + date +
                ", version=" + version +
                ", isDetailed=" + isDetailedResponse() +
                ", hasCosts=" + hasCostInformation() +
                ", hasWarnings=" + hasWarnings() +
                ", isEditable=" + isEditable() +
                ", totalCost=" + getTotalCost() +
                ", summary='" + getWorkDaySummary() + '\'' +
                '}';
    }
}