package com.gardening.timemanagement.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "employee_times",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"work_day_id", "employee_id"},
                name = "uk_workday_employee"
        ))
public class EmployeeTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relation till arbetsdagen som denna arbetstidsregistrering tillhör.
     * Many-to-One eftersom en arbetsdag kan ha många medarbetartider.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_day_id", nullable = false)
    @NotNull(message = "Arbetsdag måste anges")
    private WorkDay workDay;

    /**
     * Relation till medarbetaren som denna arbetstid gäller.
     * Many-to-One eftersom en medarbetare kan ha många arbetstidsregistreringar.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @NotNull(message = "Medarbetare måste anges")
    private Employee employee;

    /**
     * Tid när medarbetaren började arbeta.
     * Används för att beräkna total arbetad tid och för schemavalidering.
     */
    @Column(name = "start_time", nullable = false)
    @NotNull(message = "Starttid måste anges")
    private LocalTime startTime;

    /**
     * Tid när medarbetaren slutade arbeta.
     * Måste vara efter starttid för att ge giltig arbetstid.
     */
    @Column(name = "end_time", nullable = false)
    @NotNull(message = "Sluttid måste anges")
    private LocalTime endTime;

    /**
     * Lunchtid i minuter som ska dras av från den totala arbetstiden.
     * Standardvärde är 0 för de dagar där ingen lunch tas eller rapporteras.
     */
    @Column(name = "lunch_minutes", nullable = false)
    @Min(value = 0, message = "Lunchtid kan inte vara negativ")
    private Integer lunchMinutes = 0;

    /**
     * Indikerar om medarbetaren var förare denna dag.
     * Förare får registrera extra körtid som läggs till den ordinarie arbetstiden.
     */
    @Column(name = "is_driver", nullable = false)
    private Boolean isDriver = false;

    /**
     * Antal timmar körtid (utöver ordinarie arbetstid) för förare.
     * Endast relevant när isDriver är true.
     * Använder BigDecimal för exakt precision i tidsberäkningar.
     */
    @Column(name = "drive_time_hours", precision = 4, scale = 2)
    @Min(value = 0, message = "Körtid kan inte vara negativ")
    private BigDecimal driveTimeHours = BigDecimal.ZERO;

    // Default konstruktor för JPA
    public EmployeeTime() {}

    /**
     * Konstruktor för att skapa en ny arbetstidsregistrering.
     */
    public EmployeeTime(WorkDay workDay, Employee employee, LocalTime startTime,
                        LocalTime endTime, Integer lunchMinutes) {
        this.workDay = workDay;
        this.employee = employee;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lunchMinutes = lunchMinutes != null ? lunchMinutes : 0;
    }


    // Konstruktor för att registrera körtid för förare
    public EmployeeTime(WorkDay workDay, Employee employee, LocalTime startTime,
                        LocalTime endTime, Integer lunchMinutes,
                        Boolean isDriver, BigDecimal driveTimeHours) {
        this(workDay, employee, startTime, endTime, lunchMinutes);
        this.isDriver = isDriver != null ? isDriver : false;
        this.driveTimeHours = (this.isDriver && driveTimeHours != null) ?
                driveTimeHours : BigDecimal.ZERO;
    }

    // Getter och setter metoder
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkDay getWorkDay() {
        return workDay;
    }

    public void setWorkDay(WorkDay workDay) {
        this.workDay = workDay;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
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
        // Om inte längre förare, nollställ körtid
        if (!Boolean.TRUE.equals(isDriver)) {
            this.driveTimeHours = BigDecimal.ZERO;
        }
    }

    public BigDecimal getDriveTimeHours() {
        return driveTimeHours;
    }

    public void setDriveTimeHours(BigDecimal driveTimeHours) {

        // Körtid kan bara sättas om medarbetaren är förare
        if (Boolean.TRUE.equals(isDriver) && driveTimeHours != null) {
            this.driveTimeHours = driveTimeHours;
        } else {
            this.driveTimeHours = BigDecimal.ZERO;
        }
    }

    /**
     * Beräknar arbetade timmar (exklusive lunch men inklusive körtid).
     * Detta är den totala tiden som medarbetaren ska få betalt för.
     */
    public BigDecimal getTotalHours() {
        if (startTime == null || endTime == null) {
            return BigDecimal.ZERO;
        }

        // Beräkna arbetstid i minuter
        long workMinutes = ChronoUnit.MINUTES.between(startTime, endTime);

        // Dra av lunchtid
        long lunchMins = lunchMinutes != null ? lunchMinutes : 0;
        long netWorkMinutes = workMinutes - lunchMins;

        // Konvertera till timmar
        BigDecimal workHours = BigDecimal.valueOf(netWorkMinutes).divide(
                BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);

        // Lägg till körtid om förare
        if (Boolean.TRUE.equals(isDriver) && driveTimeHours != null) {
            workHours = workHours.add(driveTimeHours);
        }

        return workHours.max(BigDecimal.ZERO); // Aldrig negativ tid
    }

    /**
     * Beräknar endast ordinarie arbetstid (utan körtid).
     * Användbart för rapporter som ska skilja på arbetstid och körtid.
     */
    public BigDecimal getRegularWorkHours() {
        if (startTime == null || endTime == null) {
            return BigDecimal.ZERO;
        }

        long workMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        long lunchMins = lunchMinutes != null ? lunchMinutes : 0;
        long netWorkMinutes = workMinutes - lunchMins;

        BigDecimal workHours = BigDecimal.valueOf(netWorkMinutes).divide(
                BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);

        return workHours.max(BigDecimal.ZERO);
    }


    public boolean isValidTimeEntry() {
        if (startTime == null || endTime == null) {
            return false;
        }

        // Sluttid måste vara efter starttid
        if (!endTime.isAfter(startTime)) {
            return false;
        }

        // Lunchtid kan inte vara längre än arbetstiden
        long workMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (lunchMinutes != null && lunchMinutes > workMinutes) {
            return false;
        }

        // Körtid ska bara finnas för förare
        if (!Boolean.TRUE.equals(isDriver) && driveTimeHours != null &&
                driveTimeHours.compareTo(BigDecimal.ZERO) > 0) {
            return false;
        }

        return true;
    }


    // Kontrollerar om detta är en rimlig arbetsdag (inte för lång eller kort)
    public boolean isReasonableWorkDay() {
        BigDecimal totalHours = getTotalHours();

        // Mellan 0.5 och 16 timmar anses rimligt
        return totalHours.compareTo(BigDecimal.valueOf(0.5)) >= 0 &&
                totalHours.compareTo(BigDecimal.valueOf(16)) <= 0;
    }

    /**
     * Returnerar en beskrivning av arbetstiden för rapporter.
     */
    public String getWorkTimeDescription() {
        if (!isValidTimeEntry()) {
            return "Ogiltig tidsregistrering";
        }

        StringBuilder desc = new StringBuilder();
        desc.append(startTime).append(" - ").append(endTime);

        if (lunchMinutes != null && lunchMinutes > 0) {
            desc.append(" (lunch: ").append(lunchMinutes).append(" min)");
        }

        if (Boolean.TRUE.equals(isDriver) && driveTimeHours != null &&
                driveTimeHours.compareTo(BigDecimal.ZERO) > 0) {
            desc.append(" + körtid: ").append(driveTimeHours).append("h");
        }

        desc.append(" = ").append(getTotalHours()).append("h totalt");

        return desc.toString();
    }

    /**
     * Uppdaterar körtid med validering.
     * Säkerställer att endast förare kan ha körtid.
     */
    public void updateDriveTime(BigDecimal newDriveTime) {
        if (!Boolean.TRUE.equals(isDriver)) {
            throw new IllegalStateException("Endast förare kan registrera körtid");
        }

        if (newDriveTime == null || newDriveTime.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Körtid kan inte vara negativ");
        }

        this.driveTimeHours = newDriveTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeTime)) return false;

        EmployeeTime that = (EmployeeTime) o;

        // Två EmployeeTime är lika om de har samma workDay och employee
        // Detta förhindrar dubbletter av samma medarbetare på samma arbetsdag
        return workDay != null && workDay.equals(that.workDay) &&
                employee != null && employee.equals(that.employee);
    }

    @Override
    public int hashCode() {
        int result = workDay != null ? workDay.hashCode() : 0;
        result = 31 * result + (employee != null ? employee.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EmployeeTime{" +
                "id=" + id +
                ", workDay=" + (workDay != null ? workDay.getId() : "null") +
                ", employee=" + (employee != null ? employee.getName() : "null") +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", lunchMinutes=" + lunchMinutes +
                ", isDriver=" + isDriver +
                ", driveTimeHours=" + driveTimeHours +
                ", totalHours=" + getTotalHours() +
                '}';
    }
}
