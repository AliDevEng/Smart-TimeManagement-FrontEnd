// EmployeeMonthlyReportDto.java
package com.gardening.timemanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.YearMonth;

/**
 * DTO för månadsrapport för medarbetare.
 * Innehåller all nödvändig information för löneberäkning och statistik.
 */
public class EmployeeMonthlyReportDto {

    private Long employeeId;
    private String employeeName;
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM")
    private YearMonth month;

    private Long workDays;
    private Double totalHours;
    private Double regularHours;
    private Double overtimeHours;
    private Double totalDriveHours;
    private Double averageHoursPerDay;

    // Beräknade fält för användargränssnitt
    private Double overtimePercentage;
    private String workDaysDescription;
    private String totalHoursFormatted;

    public EmployeeMonthlyReportDto() {}

    public EmployeeMonthlyReportDto(Long employeeId, String employeeName, Boolean isActive,
                                    YearMonth month, Long workDays, Double totalHours,
                                    Double regularHours, Double overtimeHours,
                                    Double totalDriveHours, Double averageHoursPerDay) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.isActive = isActive;
        this.month = month;
        this.workDays = workDays;
        this.totalHours = totalHours;
        this.regularHours = regularHours;
        this.overtimeHours = overtimeHours;
        this.totalDriveHours = totalDriveHours;
        this.averageHoursPerDay = averageHoursPerDay;

        calculateDerivedFields();
    }

    private void calculateDerivedFields() {
        this.overtimePercentage = totalHours != null && totalHours > 0 ?
                (overtimeHours != null ? (overtimeHours / totalHours) * 100 : 0) : 0;
        this.workDaysDescription = (workDays != null ? workDays : 0) + " arbetsdagar";
        this.totalHoursFormatted = String.format("%.1f timmar", totalHours != null ? totalHours : 0);
    }

    // Getters och setters
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public YearMonth getMonth() { return month; }
    public void setMonth(YearMonth month) { this.month = month; }

    public Long getWorkDays() { return workDays; }
    public void setWorkDays(Long workDays) { this.workDays = workDays; }

    public Double getTotalHours() { return totalHours; }
    public void setTotalHours(Double totalHours) { this.totalHours = totalHours; }

    public Double getRegularHours() { return regularHours; }
    public void setRegularHours(Double regularHours) { this.regularHours = regularHours; }

    public Double getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(Double overtimeHours) { this.overtimeHours = overtimeHours; }

    public Double getTotalDriveHours() { return totalDriveHours; }
    public void setTotalDriveHours(Double totalDriveHours) { this.totalDriveHours = totalDriveHours; }

    public Double getAverageHoursPerDay() { return averageHoursPerDay; }
    public void setAverageHoursPerDay(Double averageHoursPerDay) { this.averageHoursPerDay = averageHoursPerDay; }

    public Double getOvertimePercentage() { return overtimePercentage; }
    public String getWorkDaysDescription() { return workDaysDescription; }
    public String getTotalHoursFormatted() { return totalHoursFormatted; }

    @Override
    public String toString() {
        return "EmployeeMonthlyReportDto{" +
                "employeeId=" + employeeId +
                ", employeeName='" + employeeName + '\'' +
                ", month=" + month +
                ", workDays=" + workDays +
                ", totalHours=" + totalHours +
                ", overtimeHours=" + overtimeHours +
                '}';
    }
}