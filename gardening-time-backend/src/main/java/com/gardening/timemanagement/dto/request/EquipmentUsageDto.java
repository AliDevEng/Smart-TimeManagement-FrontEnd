package com.gardening.timemanagement.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Objects;


public class EquipmentUsageDto {

    /**
     * ID för utrustningen som används.
     * Måste referera till befintlig och tillgänglig utrustning.
     */
    @NotNull(message = "Utrustnings-ID måste anges")
    @Positive(message = "Utrustnings-ID måste vara ett positivt tal")
    private Long equipmentId;

    /**
     * Antal timmar som utrustningen används (valfritt).
     * Om detta inte anges antas utrustningen användas hela dagen.
     * Används för detaljerad kostnadsberäkning.
     */
    @DecimalMin(value = "0.0", message = "Användningstid kan inte vara negativ")
    @DecimalMax(value = "24.0", message = "Användningstid kan inte vara längre än 24 timmar")
    private BigDecimal usageHours;

    /**
     * Indikerar om utrustningen används hela dagen.
     * Om true, ignoreras usageHours och full dagskostnad debiteras.
     * Om false, används usageHours för proportionell beräkning.
     */
    private Boolean isFullDayUsage;

    /**
     * Valfria anteckningar om utrustningsanvändningen.
     * Kan innehålla information om skick, problem, speciell användning etc.
     */
    @Size(max = 500, message = "Anteckningar får inte vara längre än 500 tecken")
    private String notes;

    /**
     * Prioritet för utrustningen (valfritt).
     * Används för att indikera hur kritisk utrustningen är för arbetet.
     * Kan vara användbart för schemaläggning och resursallokering.
     */
    @Min(value = 1, message = "Prioritet måste vara mellan 1 och 5")
    @Max(value = 5, message = "Prioritet måste vara mellan 1 och 5")
    private Integer priority;

    // =================================================================
    // KONSTRUKTORER
    // =================================================================

    /**
     * Standardkonstruktor för JSON-deserialisering.
     */
    public EquipmentUsageDto() {
        this.isFullDayUsage = true; // Default till heldagsanvändning
        this.priority = 3; // Medium prioritet som default
    }

    /**
     * Konstruktor för enkel heldagsanvändning.
     * Detta är den vanligaste formen av utrustningsregistrering.
     */
    public EquipmentUsageDto(Long equipmentId) {
        this.equipmentId = equipmentId;
        this.isFullDayUsage = true;
        this.usageHours = null; // Irrelevant för heldagsanvändning
        this.priority = 3;
    }

    /**
     * Konstruktor för specifik användningstid.
     * Används när utrustning endast behövs delar av dagen.
     */
    public EquipmentUsageDto(Long equipmentId, BigDecimal usageHours) {
        this.equipmentId = equipmentId;
        this.usageHours = usageHours;
        this.isFullDayUsage = false;
        this.priority = 3;
    }

    /**
     * Konstruktor för komplett utrustningsregistrering med alla detaljer.
     */
    public EquipmentUsageDto(Long equipmentId, BigDecimal usageHours, Boolean isFullDayUsage,
                             String notes, Integer priority) {
        this.equipmentId = equipmentId;
        this.usageHours = usageHours;
        this.isFullDayUsage = isFullDayUsage != null ? isFullDayUsage : true;
        this.notes = notes;
        this.priority = priority != null ? priority : 3;
    }

    // =================================================================
    // BUSINESS LOGIC BERÄKNINGAR
    // =================================================================

    /**
     * Beräknar effektiv användningstid för kostnadsberäkning.
     *
     * Om isFullDayUsage är true returneras null för att indikera
     * att full dagskostnad ska användas. Annars returneras det
     * angivna antalet timmar.
     */
    public BigDecimal getEffectiveUsageHours() {
        if (Boolean.TRUE.equals(isFullDayUsage)) {
            return null; // Indikerar full dagskostnad
        }
        return usageHours;
    }

    /**
     * Kontrollerar om användningen är giltig enligt affärsregler.
     *
     * Validerar att antingen heldagsanvändning är markerat ELLER
     * att specifika användningstimmar är angivna, men inte båda.
     */
    public boolean isValidUsageConfiguration() {
        // Grundläggande utrustnings-ID måste finnas
        if (equipmentId == null || equipmentId <= 0) {
            return false;
        }

        // Om heldagsanvändning, ska inga specifika timmar anges
        if (Boolean.TRUE.equals(isFullDayUsage)) {
            // Det är OK om usageHours är null eller zero för heldagsanvändning
            return usageHours == null || usageHours.compareTo(BigDecimal.ZERO) == 0;
        }

        // Om inte heldagsanvändning, måste specifika timmar anges
        if (Boolean.FALSE.equals(isFullDayUsage)) {
            return usageHours != null && usageHours.compareTo(BigDecimal.ZERO) > 0;
        }

        // Om isFullDayUsage är null, tolka baserat på usageHours
        return usageHours != null && usageHours.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returnerar en beskrivning av användningen för rapporter.
     *
     * Skapar en human-readable beskrivning som kan användas i
     * användargränssnitt och rapporter.
     */
    public String getUsageDescription() {
        if (Boolean.TRUE.equals(isFullDayUsage)) {
            return "Heldagsanvändning";
        } else if (usageHours != null) {
            return usageHours + " timmar";
        } else {
            return "Okänd användning";
        }
    }

    /**
     * Returnerar prioritetsbeskrivning för användargränssnitt.
     */
    public String getPriorityDescription() {
        if (priority == null) return "Medium";

        return switch (priority) {
            case 1 -> "Mycket låg";
            case 2 -> "Låg";
            case 3 -> "Medium";
            case 4 -> "Hög";
            case 5 -> "Kritisk";
            default -> "Medium";
        };
    }

    // =================================================================
    // GETTERS OCH SETTERS
    // =================================================================

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public BigDecimal getUsageHours() {
        return usageHours;
    }

    public void setUsageHours(BigDecimal usageHours) {
        this.usageHours = usageHours;
        // Om specifika timmar anges, sätt isFullDayUsage till false
        if (usageHours != null && usageHours.compareTo(BigDecimal.ZERO) > 0) {
            this.isFullDayUsage = false;
        }
    }

    public Boolean getIsFullDayUsage() {
        return isFullDayUsage;
    }

    public void setIsFullDayUsage(Boolean isFullDayUsage) {
        this.isFullDayUsage = isFullDayUsage;
        // Om heldagsanvändning aktiveras, nollställ specifika timmar
        if (Boolean.TRUE.equals(isFullDayUsage)) {
            this.usageHours = null;
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    // =================================================================
    // UTILITY METODER
    // =================================================================

    @Override
    public String toString() {
        return "EquipmentUsageDto{" +
                "equipmentId=" + equipmentId +
                ", usageHours=" + usageHours +
                ", isFullDayUsage=" + isFullDayUsage +
                ", notes='" + notes + '\'' +
                ", priority=" + priority +
                ", description='" + getUsageDescription() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EquipmentUsageDto that = (EquipmentUsageDto) o;
        return Objects.equals(equipmentId, that.equipmentId) &&
                Objects.equals(usageHours, that.usageHours) &&
                Objects.equals(isFullDayUsage, that.isFullDayUsage) &&
                Objects.equals(notes, that.notes) &&
                Objects.equals(priority, that.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(equipmentId, usageHours, isFullDayUsage, notes, priority);
    }
}
