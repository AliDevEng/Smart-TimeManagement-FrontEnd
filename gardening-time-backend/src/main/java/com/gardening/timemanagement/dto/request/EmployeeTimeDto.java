package com.gardening.timemanagement.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * DTO som representerar en medarbetares arbetstid för en specifik arbetsdag.
 *
 * Denna klass kapslar in all information som rör en medarbetares närvaro
 * och arbetstid under en arbetsdag. Den hanterar både grundläggande arbetstid
 * och specialfall som körtid för förare.
 *
 * Designprinciper:
 * - Enkel datarepresentation utan komplex affärslogik
 * - Automatisk beräkning av totaltid baserat på start/slut-tider
 * - Specialhantering av körtid för medarbetare som fungerar som förare
 * - Robust validering av tidslogik
 */
public class EmployeeTimeDto {

    /**
     * ID för medarbetaren som arbetar.
     * Måste referera till en befintlig och aktiv medarbetare.
     */
    @NotNull(message = "Medarbetar-ID måste anges")
    @Positive(message = "Medarbetar-ID måste vara ett positivt tal")
    private Long employeeId;

    /**
     * Starttid för arbetet denna dag.
     * Används tillsammans med sluttid för att beräkna total arbetstid.
     */
    @NotNull(message = "Starttid måste anges")
    private LocalTime startTime;

    /**
     * Sluttid för arbetet denna dag.
     * Måste vara efter starttid för att ge en giltig arbetstid.
     */
    @NotNull(message = "Sluttid måste anges")
    private LocalTime endTime;

    /**
     * Lunchtid i minuter (valfritt).
     * Dras av från total arbetstid om den anges.
     */
    @Min(value = 0, message = "Lunchtid kan inte vara negativ")
    @Max(value = 480, message = "Lunchtid kan inte vara längre än 8 timmar")
    private Integer lunchMinutes;

    /**
     * Indikerar om denna medarbetare fungerar som förare denna dag.
     * Förare kan registrera körtid utöver vanlig arbetstid.
     */
    private Boolean isDriver;

    /**
     * Körtid i timmar (endast för förare).
     * Kan endast anges om isDriver är true.
     */
    @DecimalMin(value = "0.0", message = "Körtid kan inte vara negativ")
    @DecimalMax(value = "24.0", message = "Körtid kan inte vara längre än 24 timmar")
    private BigDecimal driveTimeHours;

    // =================================================================
    // KONSTRUKTORER
    // =================================================================

    /**
     * Standardkonstruktor för JSON-deserialisering.
     */
    public EmployeeTimeDto() {
        this.isDriver = false;
        this.driveTimeHours = BigDecimal.ZERO;
        this.lunchMinutes = 0;
    }

    /**
     * Konstruktor för vanliga arbetsdagar utan körtid.
     */
    public EmployeeTimeDto(Long employeeId, LocalTime startTime, LocalTime endTime, Integer lunchMinutes) {
        this.employeeId = employeeId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lunchMinutes = lunchMinutes != null ? lunchMinutes : 0;
        this.isDriver = false;
        this.driveTimeHours = BigDecimal.ZERO;
    }

    /**
     * Komplett konstruktor inklusive körtid för förare.
     */
    public EmployeeTimeDto(Long employeeId, LocalTime startTime, LocalTime endTime,
                           Integer lunchMinutes, Boolean isDriver, BigDecimal driveTimeHours) {
        this.employeeId = employeeId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lunchMinutes = lunchMinutes != null ? lunchMinutes : 0;
        this.isDriver = isDriver != null ? isDriver : false;
        this.driveTimeHours = (Boolean.TRUE.equals(isDriver) && driveTimeHours != null)
                ? driveTimeHours : BigDecimal.ZERO;
    }

    // =================================================================
    // BUSINESS LOGIC BERÄKNINGAR
    // =================================================================

    /**
     * Beräknar total arbetstid i timmar.
     *
     * Denna metod tar hänsyn till starttid, sluttid och lunchtid för att
     * ge en korrekt representation av faktisk arbetstid. Resultatet används
     * för löneberäkningar och rapporter.
     */
    public BigDecimal calculateTotalHours() {
        if (startTime == null || endTime == null) {
            return BigDecimal.ZERO;
        }

        // Beräkna total tid mellan start och slut
        long totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime);

        // Dra av lunchtid om den anges
        if (lunchMinutes != null && lunchMinutes > 0) {
            totalMinutes -= lunchMinutes;
        }

        // Konvertera till timmar med decimal precision
        BigDecimal hours = BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);

        // Säkerställ att resultatet inte är negativt
        return hours.max(BigDecimal.ZERO);
    }

    /**
     * Beräknar total tid inklusive körtid för förare.
     *
     * För förare adderas körtiden till den ordinarie arbetstiden.
     * Detta ger den totala tiden som medarbetaren ska kompenseras för.
     */
    public BigDecimal calculateTotalTimeIncludingDriving() {
        BigDecimal workHours = calculateTotalHours();

        if (Boolean.TRUE.equals(isDriver) && driveTimeHours != null) {
            return workHours.add(driveTimeHours);
        }

        return workHours;
    }

    /**
     * Validerar att tidslogiken är korrekt.
     *
     * Kontrollerar att sluttid är efter starttid, att lunchtid inte är längre
     * än arbetstid, och att körtid endast anges för förare.
     */
    public boolean isValidTimeLogic() {
        // Grundläggande tider måste finnas
        if (startTime == null || endTime == null) {
            return false;
        }

        // Sluttid måste vara efter starttid
        if (!endTime.isAfter(startTime)) {
            return false;
        }

        // Lunchtid får inte vara längre än arbetstid
        if (lunchMinutes != null && lunchMinutes > 0) {
            long totalWorkMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
            if (lunchMinutes > totalWorkMinutes) {
                return false;
            }
        }

        // Körtid får endast anges för förare
        if (!Boolean.TRUE.equals(isDriver) && driveTimeHours != null &&
                driveTimeHours.compareTo(BigDecimal.ZERO) > 0) {
            return false;
        }

        return true;
    }

    // =================================================================
    // GETTERS OCH SETTERS
    // =================================================================

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getLunchMinutes() {
        return lunchMinutes;
    }

    public void setLunchMinutes(Integer lunchMinutes) {
        this.lunchMinutes = lunchMinutes;
    }

    public Boolean getIsDriver() {
        return isDriver;
    }

    public void setIsDriver(Boolean isDriver) {
        this.isDriver = isDriver;
        // Automatiskt nollställ körtid om inte längre förare
        if (!Boolean.TRUE.equals(isDriver)) {
            this.driveTimeHours = BigDecimal.ZERO;
        }
    }

    public BigDecimal getDriveTimeHours() {
        return driveTimeHours;
    }

    public void setDriveTimeHours(BigDecimal driveTimeHours) {
        // Körtid kan bara sättas för förare
        if (Boolean.TRUE.equals(isDriver) && driveTimeHours != null) {
            this.driveTimeHours = driveTimeHours;
        } else {
            this.driveTimeHours = BigDecimal.ZERO;
        }
    }

    // =================================================================
    // UTILITY METODER
    // =================================================================

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmployeeTimeDto that = (EmployeeTimeDto) o;
        return Objects.equals(employeeId, that.employeeId) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(lunchMinutes, that.lunchMinutes) &&
                Objects.equals(isDriver, that.isDriver) &&
                Objects.equals(driveTimeHours, that.driveTimeHours);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, startTime, endTime, lunchMinutes, isDriver, driveTimeHours);
    }
}

