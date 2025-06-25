package com.gardening.timemanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gardening.timemanagement.entity.Task.TaskStatus;
import com.gardening.timemanagement.util.TaskStatusUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@JsonInclude(JsonInclude.Include.NON_NULL) // Exkludera null-värden från JSON-output
public class TaskResponseDto {

    private Long id;

    private String number;

    private TaskStatus status;

    private String statusDisplay;


    private String description;

    /**
     * Datum när uppdraget startades (eller planerat startdatum).
     * Vi formaterar detta explicit för konsistent date-hantering över API:et.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * Datum när uppdraget avslutades.
     * Detta är bara ifyllt för COMPLETED eller CANCELLED uppdrag.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * Beräknat fält: antal dagar uppdraget har pågått eller totalt varade.
     * Detta är ett exempel på hur vi kan inkludera beräknade värden i våra
     * response DTOs för att förbättra användarupplevelsen.
     */
    private Long durationInDays;

    /**
     * Embedded customer-information.
     * Istället för att bara inkludera customer ID inkluderar vi nödvändig
     * kunddata för att undvika extra API-anrop från frontend.
     *
     * Detta är en conscious performance/convenience trade-off där vi väljer
     * att inkludera mer data i varje response för att minska total API-traffic.
     */
    private CustomerSummary customer;

    /**
     * Systemmetadata: När uppdraget skapades i systemet.
     * Användbara för administrativ spårning och sortering.
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Systemmetadata: När uppdraget senast uppdaterades.
     * Användbart för caching, konfliktdetektering, och audit trails.
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Beräknat fält: Indikerar om uppdraget kan ta emot ny arbetstidsregistrering.
     * Detta är en kritisk affärslogik som frontend behöver för att visa
     * rätt användargränssnittsalternativ.
     */
    private Boolean canAcceptWorkTime;

    /**
     * Beräknat fält: Indikerar om uppdraget är i ett slutgiltigt tillstånd.
     * Användbart för UI-logik kring redigering och statusändringar.
     */
    private Boolean isFinalized;

    /**
     * Beräknat fält: Lista över tillåtna statusövergångar från nuvarande status.
     * Detta ger frontend exakt information om vilka statusändringar som är
     * tillåtna utan att behöva duplicera affärslogik på klient-sidan.
     */
    private java.util.Set<TaskStatus> allowedStatusTransitions;

    // Default konstruktor för JSON serialization
    public TaskResponseDto() {}

    /**
     * Huvudkonstruktor som används av vår TaskMapper.
     * Inkluderar all grundläggande information och beräknar derived fields.
     */
    public TaskResponseDto(Long id, String number, TaskStatus status, String description,
                           LocalDate startDate, LocalDate endDate, CustomerSummary customer,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.number = number;
        this.status = status;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.customer = customer;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

        // Beräkna derived fields baserat på grunddata
        calculateDerivedFields();
    }

    /**
     * Beräknar alla derived/calculated fields baserat på grunddata.
     * Denna metod anropas efter att grunddata har satts för att säkerställa
     * konsistens mellan alla beräknade värden.
     */
    private void calculateDerivedFields() {
        // Status-relaterade beräkningar
        this.statusDisplay = TaskStatusUtils.getDisplayName(status);
        this.canAcceptWorkTime = TaskStatusUtils.canAcceptWorkTime(status);
        this.isFinalized = TaskStatusUtils.isFinalized(status);
        this.allowedStatusTransitions = TaskStatusUtils.getValidTransitions(status);

        // Datum och varaktighet
        calculateDuration();
    }

    /**
     * Beräknar uppdragets varaktighet baserat på start- och slutdatum.
     * Hanterar olika scenarion: pågående uppdrag, avslutade uppdrag, inte startade.
     */
    private void calculateDuration() {
        if (startDate == null) {
            this.durationInDays = 0L;
            return;
        }

        LocalDate endDateForCalculation = endDate != null ? endDate : LocalDate.now();
        this.durationInDays = ChronoUnit.DAYS.between(startDate, endDateForCalculation) + 1;

        // Säkerställ att varaktigheten aldrig är negativ (för felaktiga data)
        if (this.durationInDays < 0) {
            this.durationInDays = 0L;
        }
    }

    // Getters och setters med smart update-logik

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public TaskStatus getStatus() { return status; }

    /**
     * Smart setter för status som automatiskt uppdaterar alla relaterade derived fields.
     */
    public void setStatus(TaskStatus status) {
        this.status = status;
        // Uppdatera alla status-relaterade derived fields
        if (status != null) {
            this.statusDisplay = TaskStatusUtils.getDisplayName(status);
            this.canAcceptWorkTime = TaskStatusUtils.canAcceptWorkTime(status);
            this.isFinalized = TaskStatusUtils.isFinalized(status);
            this.allowedStatusTransitions = TaskStatusUtils.getValidTransitions(status);
        }
    }

    public String getStatusDisplay() { return statusDisplay; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }

    /**
     * Smart setter för startDate som automatiskt omberäknar varaktighet.
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        calculateDuration();
    }

    public LocalDate getEndDate() { return endDate; }

    /**
     * Smart setter för endDate som automatiskt omberäknar varaktighet.
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        calculateDuration();
    }

    public Long getDurationInDays() { return durationInDays; }

    public CustomerSummary getCustomer() { return customer; }
    public void setCustomer(CustomerSummary customer) { this.customer = customer; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getCanAcceptWorkTime() { return canAcceptWorkTime; }
    public Boolean getIsFinalized() { return isFinalized; }
    public java.util.Set<TaskStatus> getAllowedStatusTransitions() { return allowedStatusTransitions; }

    /**
     * Convenience-metod för att få en läsbar sammanfattning av uppdraget.
     * Användbar för loggning, debugging, och användarnotifikationer.
     */
    public String getDisplaySummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Uppdrag ").append(number);

        if (customer != null) {
            summary.append(" för ").append(customer.getName());
        }

        summary.append(" (").append(statusDisplay).append(")");

        if (durationInDays != null && durationInDays > 0) {
            summary.append(" - ").append(durationInDays).append(" dagar");
        }

        return summary.toString();
    }

    /**
     * Kontrollerar om detta uppdrag kan redigeras baserat på dess status och andra faktorer.
     * Användbart för frontend-logik kring edit-funktionalitet.
     */
    public boolean isEditable() {
        return status == TaskStatus.ACTIVE;
    }

    /**
     * Returnerar färgkod för statusvisning i användargränssnitt.
     * Detta är ett exempel på hur vi kan inkludera UI-hints i våra DTOs
     * för konsistent presentation across different frontend implementations.
     */
    public String getStatusColorCode() {
        return switch (status) {
            case ACTIVE -> "#28a745";      // Grön för aktiva uppdrag
            case COMPLETED -> "#007bff";   // Blå för avslutade uppdrag
            case CANCELLED -> "#dc3545";   // Röd för avbrutna uppdrag
        };
    }

    @Override
    public String toString() {
        return "TaskResponseDto{" +
                "id=" + id +
                ", number='" + number + '\'' +
                ", status=" + status +
                ", customer=" + (customer != null ? customer.getName() : "null") +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", durationInDays=" + durationInDays +
                '}';
    }

    /**
     * Nested DTO för customer-information.
     *
     * Denna inner class representerar optimal information architecture:
     * den inkluderar precis den kundinformation som frontend behöver för
     * att visa uppdrag utan att ladda hela Customer-objektet från databasen.
     *
     * Detta är ett exempel på strategic data modeling för API performance.
     */
    public static class CustomerSummary {
        private Long id;
        private String name;
        private String phone;

        // Default konstruktor för JSON serialization
        public CustomerSummary() {}

        public CustomerSummary(Long id, String name, String phone) {
            this.id = id;
            this.name = name;
            this.phone = phone;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        /**
         * Skapar en kort kundidentifierare för användargränssnitt.
         */
        public String getDisplayName() {
            StringBuilder display = new StringBuilder(name);
            if (phone != null && !phone.trim().isEmpty()) {
                display.append(" (").append(phone).append(")");
            }
            return display.toString();
        }

        @Override
        public String toString() {
            return "CustomerSummary{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", phone='" + phone + '\'' +
                    '}';
        }
    }
}